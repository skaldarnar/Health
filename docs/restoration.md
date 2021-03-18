# Restoration

Handles magical healing of entities. 
To heal an entity, send `DoRestoreEvent(amount, instigatorRef)`. 

The event chain of restoration:

* DoRestoreEvent
* BeforeRestoreEvent 
* Entity restored, health component saved
* OnRestoreEvent
* OnFullyHealedEvent (if healed to full health)