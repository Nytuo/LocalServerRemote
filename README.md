# LocalServerRemote

This little Android Application is focused on having a simple server management for local use.
Just to be able to turn on and off your home server easly.

## Goal
A 'Power on' button that wakeup the device using the WOL (Wake On Lan) protocol.
A 'Power off' button that shutdown the device using an SSH Connection (username and password only, need to make the shutdown command not to require sudo to work)
A status indicator of the device's power state (check if a URL is reachable)

## Settings
All the configuration is handle on the app itself in the settings tab

## Security Risk

This app is made only for local home server, do NOT use it on a out-of-site purpose since the SSH connection don't use a key but a password and the WOL may not work.
