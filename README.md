Health
=======

This module handles regeneration, restoration and damage of entities.

## Regeneration
Handles the natural healing of entities (and blocks).
To activate regeneration send `ActivateRegenEvent(String id, float value, float endTime)`. Health is regenerated every 
second. Empty event `ActivateRegenEvent()` activates base regeneration of entity.

To deactivate particular type of regeneration, send `DeactivateRegenEvent(String id)`. Empty event 
`DeactivateRegenEvent()` deactivates base regeneration fo entity.

## Restoration
Handles magical healing of entities. 
To heal an entity, send `DoRestoreEvent(amount, instigatorRef)`. 

The event chain of restoration:
* DoRestoreEvent
* BeforeRestoreEvent 
* Entity restored, health component saved
* OnRestoreEvent
* OnFullyHealedEvent (if healed to full health)

## Damage System
Handles damage dealt to entities with health. Send `DoDamageEvent`
to deal damage to entity. 

Event chain:
* DoDamageEvent
* BeforeDamageEvent 
* Entity damaged, health component saved
* OnDamagedEvent

## Block Damage System
Enables block to sustain some damage before getting destroyed, and produces block particle effect on damage.