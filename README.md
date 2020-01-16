# Payload [![Actions Status](https://github.com/jonahseguin/Payload/workflows/Java%20CI/badge.svg)](https://github.com/jonahseguin/Payload/actions)
*Fail-safe asynchronous profile caching via Redis &amp; MongoDB in Java for Spigot*

**This README and the wiki documentation is currently out of date and needs to be re-written to reflect the new 2.0 re-write.**

## About

**Payload aims to provide an all-in-one solution for the profile caching use cases that I've reused across so many of my plugins.**  
It features *error handling*, *verbose debugging*, *an API with asynchronous events*, and *easy expandibility* through an abstract layer-based system.


## Key Features
- Asynchronous
- Supports Redis & MongoDB
- Failure handling
- User configurable settings
- Custom error & debug handling
- Two types of caches for Unique Player Profiles & Persistent or Non-Persistent Objects
- Fast & easy to use across multiple services, plugins, and servers
- Seamless integration into networks or standalone servers
- Bungee/LilyPad/etc. (any network suite) support via use of a custom handshaking protocol & Redis pub/sub

# Install
Payload is designed to be run as a plugin, not to be shaded.  The reasoning for this is that Payload provides commands for checking statuses and looking into details about databases, caches, profiles, and objects.

## Maven
To install with maven, clone the repository to your local machine first.  And then install it into your local maven repository:
- `git clone git@github.com:jonahseguin/Payload.git`
- `cd Payload`
- `mvn clean package install`

Once that has finished, you can add Payload to the dependencies in your project:
```xml
<dependency>
            <groupId>com.jonahseguin</groupId>
            <artifactId>Payload</artifactId>
            <version>3.1.0</version>
            <scope>provided</scope>
</dependency>
```

## Usage
Guides for using Payload in Profile and Object cache modes can be found on the wiki for this repository.

