# Graber Shade Driver
 Hubitat Driver For Graber Virtual Cord Z-Wave Shades

Author: Tim Yuhl (@WindowWasher on the Hubitat Community Forums)

**Features:**

1. Supports Open, Close, On, Off, Set Position, Start Position Change, and Stop Position Change commands
2. Battery Level reporting
3. Tested with Rule Machine, webCoRE and Hubitat Alexa integration

**Notes:**
1. This driver does not currently support secure communications. Security support could be added if needed for other compatible shades.  The Graber Virtual Cord Shades do not allow you to select secure inclusion, so adding security support was not a priority.

**Installation**

Hubitat Package Manager (recommended):

_HPM is an app that allows you to easily install and update custom drivers and apps on your Hubitat hub. To use HPM, you need to have it installed on your hub first._

Once you have HPM installed, follow these steps to install the "Graber Shade Driver" driver:

1. In the Hubitat interface, go to Apps and select Hubitat Package Manager.
2. Select Install, then Search by Keywords.
3. Enter Graber in the search box and click Next.
4. Select Graber Shade Driver by Tim Yuhl and click Next.
5. Follow the install instructions.

Manual Installation:

1. Install the driver code in the Drivers Code section of Hubitat.

_Notes:_

1. If you already have Graber shades installed, switch each of their drivers to use the Graber Shade Driver.
2. If you are pairing new shades, this driver should be automatically selected during the inclusion process if this driver is installed prior to pairing the new shades.

**_Please report any problems or bugs to the developer_**
