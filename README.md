# Payload
*Fail-safe asynchronous profile caching via Redis &amp; MongoDB in Java for Spigot*

## About

Payload aims to provide an all-in-one solution for the profile caching use cases that I've reused across so many of my plugins.  
It features error handling, verbose debugging, an API with asynchronous events, and easy expandibility through an abstract layer-based system.

The cache is split into "layers": Pre-Caching, Username/UUID, Local (cache), Redis, Mongo, and Profile Creation.  Each layer is attempted in the given order
asynchronously, and features error-redundancy and after-login async. error handling that allows the player to join even if their profile does not
load properly (it "halts" them until its loaded) -- of course this is configurable via a settings object that is passed into your ProfileCache.

## WIP

API usage, examples, documentation, etc.  are soon to come.  Right now the project is being converted from having an excessive complex code within the 
ProfileCache class to being spread out across multiple classes and functions to allow for more abstraction yet simplicity and organization.

The project is not currently buildable/working, however an official working release should follow shortly.
