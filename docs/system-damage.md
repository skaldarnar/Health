# Damage System

Handles damage dealt to entities with health. Send `DoDamageEvent`
to deal damage to entity. 

Event chain:
* DoDamageEvent
* BeforeDamageEvent 
* Entity damaged, health component saved
* OnDamagedEvent

Commands:
* damageResist(damagetype,percentage): gives resistance to damage (damagetype = all for total resistance).
* damageImmune(damagetype): percentage = 100 by default.
* checkResistance(): gives list of active resistance values