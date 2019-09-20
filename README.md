# Payload
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
- Multiple types of caches for Unique Player Profiles (persistant), Objects (persistant or non-persistant), and Simple Players (non-persistant)
- Fast & easy to use across multiple services, plugins, and servers
