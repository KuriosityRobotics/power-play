## NOTICE
welcome to hell!

99% of code (relevant to you 😼) is in the hardware-independent and hardware-dependent modules.  

code that directly interacts with sensors and hardware should publish sensor readings to topics, or subscribe to topics and send commands to hardware accordingly.  this code should be in hardware-dependent, but there shouldn't be much (don't be afraid to split packages and shit across two modules)

pubsub heinsoïty and some utility classes like pose and twist are located in the api module.  you probably won't need to deal with this, but be warned that the intellij plugin may or may not shit itself if you add fields to the utility classes.  methods are probably fine.

client-plugin contains ui code for the plugin.  it only depends on api;  the intellij plugin is hard to update, which is why api should preferably not be changed.  if you think that the pubsub code is deranged, definitely don't look at this
