# Payload
*Fail-safe asynchronous profile caching via Redis &amp; MongoDB in Java for Spigot*


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


## Cache Types
There are *three different types of caches* within Payload: **Profile**, **Object**, and **Simple**


### Profile
For **Profile Caching**, cache system is split into "layers": Pre-Caching, Username/UUID, Local (cache), Redis, Mongo, and Profile Creation.  

Each layer is attempted in the given order
asynchronously, and features error-redundancy and after-login asynchronous error handling that allows the player to join even if their profile does not
load properly (it "halts" them until its loaded) -- of course this is configurable via a settings object that is passed into your Payload Cache.

Get started with Profile Caching on the [Profile Caching](https://github.com/jonahseguin/Payload/wiki/Using-the-Profile-Cache) wiki page.

*The codebase for the Profile Caching system can be found in the [profile package](https://github.com/jonahseguin/Payload/tree/master/src/main/java/com/jonahseguin/payload/profile).*


### Object
Payload also now supports **object caching**.  Objects (such as Factions, Claims, or other non-player objects) can be cached and automatically handled by Payload.  They can be persistent or non-persistent.  

Like the Profile Cache, the Object Caching System also works in layers (Local -> Redis -> Mongo).  You can configure whether you want Redis & Mongo to be enabled.

Get started with Object Caching on the [Object Caching](https://github.com/jonahseguin/Payload/wiki/Using-the-Object-Cache) wiki page.

*The codebase for the Object Caching system can be found in the [object package](https://github.com/jonahseguin/Payload/tree/master/src/main/java/com/jonahseguin/payload/object).*

### Simple
This is the most, well, simple cache type.  It is a non-persistant mode that allows for the creation of local player-based objects which are cached for that session of the server being online.  Of course there are TTL settings for these objects as well.

Get started with Simple Caching on the [Simple Caching](https://github.com/jonahseguin/Payload/wiki/Using-the-Simple-Cache) wiki page.

*The codebase for the Simple Caching system can be found in the [simple package](https://github.com/jonahseguin/Payload/tree/master/src/main/java/com/jonahseguin/payload/simple).*


## Add Payload as a dependency
For information on how to install Payload as a dependency in your project, visit the [Getting Started](https://github.com/jonahseguin/Payload/wiki/Getting-Started) wiki page!


## Examples

```java
PayloadProfileCache<MyProfile> cache = ...; // learn how to setup your Profile Cache on the Profile Cache wiki page!
```

### Getting a Profile
```java
MyProfile profile = cache.getProfile(player);

MyProfile profile = cache.getProfile("uniqueId");

MyProfile profile = cache.getProfileByUsername("username");

```

### Save a profile
```java
cache.saveEverywhere(profile); // Sync
```

### Get all cached profiles
```java
cache.getCachedProfiles()

// Or get only the online cached profiles:
cache.getOnlineProfiles()

```

### Save all profiles
```java
cache.saveAll(callback -> {
    int count = callback.getKey(); // Total # profiles saved
    int failed = callback.getValue(); // # profiles that failed to save
    if (failed > 0)
        Bukkit.broadcastMessage("Warning: " + failed + " profiles failed to save!");
});
```

### Get an object: such as a Faction
```java
Faction faction = factionObjectCache.get("factionName");
```

### Get an object from a specific layer
```java
factionObjectCache.getFrom(OLayerType.REDIS, "factionName");
```

### Remove an object from the local cache
```java
factionObjectCache.uncache(faction);
// or
factionObjectCache.uncache("factionName");
```

### Save an object
```java
factionObjectCache.saveEverywhere(faction);
```

### Delete an object from everywhere (Local + Redis + Mongo)
```java
factionObjectCache.deleteEverywhere("factionName");
```

### You can even get or delete an object from a specific layer
```java
factionObjectCache.getLayerController().getLocalLayer().provide("factionName");

// or delete:
factionObjectCache.getLayerController().getLocalLayer().remove("factionName");

// from any layer
factionObjectCache.getLayerController().getRedisLayer()...
factionObjectCache.getLayerController().getMongoLayer()...
```

### You can even access the controller for each cached object to see how they were cached, or manually load the object
```java
OLayerType cacheSource = factionObjectCache.getController("factionName").getLoadedFrom(); // Local / Redis / Mongo

// or manually load it / cache it
Faction faction = factionObjectCache.getController("factionName").cache(); // If no controller exists, one is automatically created
if (faction == null) {
    // doesn't exist; create the faction [there is also a setting to allow Payload to do this automatically: 'createOnNull']
    faction = new Faction("factionName");
    factionObjectCache.saveEverywhere(faction);
}
```
