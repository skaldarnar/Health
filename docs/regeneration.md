# Regeneration

Handles the natural healing of entities (and blocks).
To activate regeneration send `ActivateRegenEvent(String id, float value, float endTime)`. 
Health is regenerated every second. 
Empty event `ActivateRegenEvent()` activates base regeneration of entity.

To deactivate particular type of regeneration, send `DeactivateRegenEvent(String id)`. 
Empty event `DeactivateRegenEvent()` deactivates base regeneration fo entity.