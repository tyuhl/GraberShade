/**
 *  Graber Shade Driver
 *
 *  Device Type:	Z-Wave Window Shade
 *  Author: 		Tim Yuhl
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 *  modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  History:
 *  7/20/21 - Initial work.
 *
 */

import groovy.transform.Field

@Field static final Map commandClassVersions = [
		0x20: 1,    //basic
		0x26: 1,    //switchMultiLevel
		0x5E: 2,    // ZwavePlusInfo
		0x80: 1     // Battery
]

String appVersion()   { return "1.0.0" }
def setVersion(){
	state.name = "Graber Shade Driver"
	state.version = "1.0.0"
}

metadata {
	definition (
			name: "Graber Shade Driver",
			namespace: "tyuhl",
			description:"Driver for Graber Z-Wave Shades",
			importUrl:"https://raw.githubusercontent.com/tyuhl/GraberShades/main/graber-shade-driver.groovy",
			author: "Tim Yuhl") {
		capability "WindowShade"
		capability "Switch"
		capability "Battery"
		capability "Initialize"
		capability "Actuator"
		capability "Sensor"
		capability "Refresh"

		fingerprint deviceId: "5A31", inClusters: "0x5E,0x26,0x85,0x59,0x72,0x86,0x5A,0x73,0x7A,0x6C,0x55,0x80", mfr: "26E", deviceJoinName: "Graber Shade"
	}
	preferences {
		section("Logging") {
			input "logging", "enum", title: "Log Level", required: false, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
		}
	}
}

/**
 * Boilerplate callback methods called by the framework
 */

void installed()
{
	log("installed() called", "trace")
	setVersion()
	initialize()
}

void updated()
{
	log("updated() called", "trace")
	setVersion()
	initialize()
}

void parse(String message)
{
	log("parse called with message: ${message}", "trace")
	hubitat.zwave.Command cmd = zwave.parse(message, commandClassVersions)
	if (cmd) {
		zwaveEvent(cmd)
	}
}

/* End of built-in callbacks */

///
// Commands
///
void initialize() {
	log("initialize() called", "trace")
	refresh()
	scheduleBatteryReport()
}

void refresh()
{
	log("refresh called", "trace")
	delayBetween([
			getBatteryReport(),
			getPositionReport()
	], 200)
}

def open() {
	log("open() called", "trace")
	setShadeLevel(99)
}

def close() {
	log("close() called", "trace")
	setShadeLevel(0)
}

def on() {
	log("on() called", "trace")
	setShadeLevel(99)
}

def off() {
	log("off() called", "trace")
	setShadeLevel(0)
}

def setPosition(value) {
	log("setPosition() called", "trace")
	setShadeLevel(value)
}

def startPositionChange(position) {
	log("startPositionChange() called with position: ${position}", "trace")
	startLevelChangeHelper(position)
}

def stopPositionChange() {
	log("stopPositionChange() called", "trace")
	try {
		sendHubCommand(new hubitat.device.HubAction(zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format(), hubitat.device.Protocol.ZWAVE))
	}
	catch(e) {
		log("unhandled error: ${e.getLocalizedMessage()}", "error")
	}
}

///
// Event Handlers
///
def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
	log("BatteryReport $cmd", "trace")
	def val = (cmd.batteryLevel == 0xFF ? 1 : cmd.batteryLevel)

	if (val > 100) val = 100
	if (val < 1) val = 1
	String disTxt = "${device.getDisplayName()} battery level is ${val}%"
	log("Battery level is ${val}%", "info")
	sendEvent(getEventMap("battery", val, "%", null, disTxt,true))
	return []
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd){
	log("SwitchMultilevelReport value: ${cmd.value}", "trace")
	shadeEvents(cmd.value,"physical")
	return []
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd){
	log("BasicReport value: ${cmd.value}", "trace")
	shadeEvents(cmd.value,"digital")
	return []
}

void zwaveEvent(hubitat.zwave.Command cmd) {
	log("Command Unhandled: $cmd", "trace")
}

///
// Supporting helpers
///
void getBatteryReport() {
	log("getBatteryReport() called", "trace")
	try {
		sendHubCommand(new hubitat.device.HubAction(zwave.batteryV1.batteryGet().format(), hubitat.device.Protocol.ZWAVE))
	}
	catch(e) {
		log("unhandled error: ${e.getLocalizedMessage()}", "error")
	}
}

private void scheduleBatteryReport() {
	unschedule(getBatteryReport)
	// Test  - every 2 minutes
	// def cronString = "0 */2 * ? * *"
	// every day at 6:00 am
	def cronString = "0 0 6 ? * *"
	log("Scheduling Battery Refresh cronstring: ${cronString}", "trace" )
	schedule(cronString, getBatteryReport)
}

private void setShadeLevel(value)
{
	Short level = Math.max(Math.min(value as Short, 99), 0)
	try {
		sendHubCommand(new hubitat.device.HubAction(zwave.switchMultilevelV1.switchMultilevelSet(value: level).format(), hubitat.device.Protocol.ZWAVE))
	}
	catch(e) {
		log("unhandled error: ${e.getLocalizedMessage()}", "error")
	}
}

private void startLevelChangeHelper(String position)
{
	Short posValue = 0
	Short curPos = device.currentValue("position")
	if (position.equalsIgnoreCase("open")) {
		posValue = 99
	} else if (position.equalsIgnoreCase("close")) {
		posValue = 0
	} else {
		throw new Exception("Invalid position value specified")
	}
	if (posValue == curPos) {
		return // nothing to be done
	}
	// false if increasing, true if decreasing
	Boolean upDn = (curPos >= posValue)

	try {
		sendHubCommand(new hubitat.device.HubAction(zwave.switchMultilevelV1.switchMultilevelStartLevelChange(ignoreStartLevel: true,
				startLevel: 0, upDown: upDn).format(), hubitat.device.Protocol.ZWAVE))
	}
	catch(e) {
		log("unhandled error: ${e.getLocalizedMessage()}", "error")
	}
}

private void getPositionReport() {
	log("getPositionReport() called", "trace")
	try {
		sendHubCommand(new hubitat.device.HubAction(zwave.switchMultilevelV1.switchMultilevelGet().format(), hubitat.device.Protocol.ZWAVE))
	}
	catch(e) {
		log("unhandled error: ${e.getLocalizedMessage()}", "error")
	}
}

private shadeEvents(value, String type) {
	Short positionVal = value
	String positionText;
	String switchText
	String shadeText
	if (positionVal == 99) {
		positionText = "${device.getDisplayName()} is open"
		switchText = "on"
		shadeText = "open"
	} else if (positionVal == 0) {
		positionText = "${device.getDisplayName()} is closed"
		switchText = "off"
		shadeText = "closed"
	} else {
		positionText = "${device.getDisplayName()}is partially open"
		shadeText = "partially open"
	}
	sendEvent(getEventMap("position", positionVal, "%", null, positionText, true))
	log("${positionText}", "debug")
	sendEvent(getEventMap("switch", switchText, null, null, null,true))
	sendEvent(getEventMap("windowShade", shadeText, null, null, positionText, true))
}

private getEventMap(name, value, unit=null, String type=null, String discText=null, displayed=false) {
	def eventMap = [
			name: name,
			value: value,
			isStateChange: true
	]
	if (unit) {
		eventMap.unit = unit
	}
	if (type) {
		eventMap.type = type
	}
	if (discText) {
		eventMap.descriptionText = discText
	}
	if (displayed) {
		eventMap.displayed = displayed
	}
	return eventMap
}

private determineLogLevel(data) {
	switch (data?.toUpperCase()) {
		case "TRACE":
			return 0
			break
		case "DEBUG":
			return 1
			break
		case "INFO":
			return 2
			break
		case "WARN":
			return 3
			break
		case "ERROR":
			return 4
			break
		default:
			return 1
	}
}

def log(Object data, String type) {
	data = "-- ${device.label} -- ${data ?: ''}"

	if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
		switch (type?.toUpperCase()) {
			case "TRACE":
				log.trace "${data}"
				break
			case "DEBUG":
				log.debug "${data}"
				break
			case "INFO":
				log.info "${data}"
				break
			case "WARN":
				log.warn "${data}"
				break
			case "ERROR":
				log.error "${data}"
				break
			default:
				log.error("-- ${device.label} -- Invalid Log Setting")
		}
	}
}