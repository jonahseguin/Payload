# Payload
*Fail-safe asynchronous profile caching via Redis &amp; MongoDB in Java for Spigot*

## About

Payload aims to provide an all-in-one solution for the profile caching use cases that I've reused across so many of my plugins.  
It features error handling, verbose debugging, an API with asynchronous events, and easy expandibility through an abstract layer-based system.

The cache is split into "layers": Pre-Caching, Username/UUID, Local (cache), Redis, Mongo, and Profile Creation.  Each layer is attempted in the given order
asynchronously, and features error-redundancy and after-login async. error handling that allows the player to join even if their profile does not
load properly (it "halts" them until its loaded) -- of course this is configurable via a settings object that is passed into your ProfileCache.

## Maven: pom.xml
Add this to your repositories:
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
and this to your dependencies:
```xml
<dependency>
    <groupId>com.github.jonahseguin</groupId>
    <artifactId>aBsorb</artifactId>
    <version>0.1</version>
</dependency>
```

## API Usage (WIP)
*More documentation is needed, as well as wiki pages, examples, etc.*

Payload provides an easy automation of Profile Caching with fail-safe checks for errors.  It handles it's own cleanup, auto-saving, errors, and more.

The user (you!) must provide information to payload in order for it to function:
- Database objects & connections (mongo+morphia, jedis (redis)) -- the libraries for these databases are shaded into Payload by default
- Settings (such as re-try intervals, whether to save/remove on logout, etc.)
- Providers for Payload functions (instantiator to create **your** Profile object when a new player joins, the profile class, debugging methods that are called by payload; you decide what to do with the information

A general example of a very basic Payload implementation can be seen in the [PayloadExample.java](https://github.com/jonahseguin/Payload/blob/master/src/main/java/com/jonahseguin/payload/PayloadExample.java)

### Creating a ProfileCache: the base of Payload

You could instantiate a ProfileClass object and pass in your settings object with your Profile Class (ie MyProfile.class -- which extends Payload's Profile class)

```java
CacheSettings settings = new MyCacheSettings(); // where MyCacheSettings extends CacheSettings and implements everything necessary
ProfileCache<MyProfile> cache = new ProfileCache<>(settings, MyProfile.class)
```
or.. you could make your life a little easier and use the builder provided by Payload to create your Cache, such as shown in the PayloadExample:

```java
ProfileCache<MyProfile> cache = new CacheBuilder<MyProfile>(this) // where this = your JavaPlugin
                .withProfileClass(MyProfile.class) // Our Profile
                .withCacheLocalExpiryMinutes(30) // Local profiles expire after 30 mins of being inactive (i.e logging out)
                .withCacheLogoutSaveDatabase(true) // Save their profile to *Mongo* (and always redis) when they logout
                .withCacheRemoveOnLogout(false) // Don't remove them from the local cache when they logout
                .withHaltListenerEnabled(true) // Allow Payload to handle the halt listener
                .withCacheFailRetryIntervalSeconds(30) // Re-try caching for fails every 30 seconds
                .withDatabase(new CacheDatabase(mongoClient, mongoDatabase, jedis, morphia, datastore)) // Pass in our database properties
                .withDebugger(new CacheDebugger() { // Handle the debug and errors provided by Payload

                    @Override
                    public void debug(String message) {
                        if (debug) {
                            getLogger().info("[Debug][Cache] " + message);
                        }
                    }

                    @Override
                    public void error(Exception ex) {
                        getLogger().info("[Error][Cache] An exception occurred: " + ex.getMessage());
                        if (debug) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void error(Exception ex, String message) {
                        getLogger().info("[Error][Cache] An exception occurred: " + message);
                        if (debug) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public boolean onStartupFailure() {
                        maintenanceMode = true; // Enable maintenance mode to prevent more players from joining

                        for (Player pl : getServer().getOnlinePlayers()) {
                            if (!pl.isOp()) {
                                pl.kickPlayer(ChatColor.RED + "The server is experiencing technical difficulties.\n" +
                                "Please join back later.");
                            }
                            else {
                                pl.sendMessage(ChatColor.RED + "All players were kicked due to a cache startup failure.");
                            }
                        }

                        return true; // Shutdown cache if it fails to start
                    }
                })
                .withInstantiator((username, uniqueId) -> new MyProfile(username, uniqueId)) // This handles the instantiation of our Profile when a new one is created
                .build(); // Done
```

Once you have created your cache, you'll want to start it up:
```java
boolean success = cache.init();
if (!success)
  doSomethingToHandleErrorWhenCacheDoesNotStartUpProperly() // or something like that... you get the idea
```

and likewise when your plugin disables:
```java
boolean success = cache.shutdown();
```

From there, the basics are done.  Payload handles the rest.

Now, how do you get a profile, you ask?  Easy.

```java
// There are several methods you can use:
MyProfile profile = cache.getProfile(player);
MyProfile profile = cache.getProfile(uniqueId);
MyProfile profile = cache.getSimpleCache().getProfileByUsername(username);
MyProfile profile = cache.getSimpleCache().getLocalProfile(player);
MyProfile profile = cache.getLayerController().getLocalLayer().get(uniqueId);
```

More examples to be added.
