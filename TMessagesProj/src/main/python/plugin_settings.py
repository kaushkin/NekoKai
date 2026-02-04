global _settings_file_path
global _settings_cache

import json
import os
import threading
import traceback
from typing import Any
from android_utils import log

_settings_cache = {}
_settings_file_path = None
_lock = threading.RLock()

def init(plugins_dir_path: str, all_shared_prefs: dict):
    global _settings_cache
    global _settings_file_path
    _settings_file_path = os.path.join(plugins_dir_path, 'plugin_settings.json')
    keys_to_delete = []
    with _lock:
        if os.path.exists(_settings_file_path):
            _load_settings_from_file()
        else:
            log('Migrating plugin settings from SharedPreferences to JSON.')
            migrated_settings = {}
            prefix = 'plugin_setting_'
            iterator = all_shared_prefs.entrySet().iterator()
            while iterator.hasNext():
                entry = iterator.next()
                key = entry.getKey()
                value = entry.getValue()
                if key.startswith(prefix):
                    parts = key[len(prefix):].split('_', 1)
                    if len(parts) == 2:
                        plugin_id, setting_key = parts
                        if plugin_id not in migrated_settings:
                            migrated_settings[plugin_id] = {}
                        migrated_settings[plugin_id][setting_key] = value
                        keys_to_delete.append(key)
            _settings_cache = migrated_settings
            _save_settings_to_file()
            log(f'Migrated settings for {len(migrated_settings)} plugins.')
    return keys_to_delete
    
def _load_settings_from_file():
    global _settings_cache
    with _lock:
        if _settings_file_path and os.path.exists(_settings_file_path):
                try:
                    with open(_settings_file_path, 'r', encoding='utf-8') as f:
                        _settings_cache = json.load(f)
                except Exception as e:
                    log(f'Error loading plugin settings from JSON: {e}\n{traceback.format_exc()}')
                    _settings_cache = {}

def _save_settings_to_file():
    with _lock:
        if _settings_file_path:
            try:
                with open(_settings_file_path, 'w', encoding='utf-8') as f:
                    json.dump(_settings_cache, f, indent=2)
            except Exception as e:
                log(f'Error saving plugin settings to JSON: {e}\n{traceback.format_exc()}')

def get_setting(plugin_id: str, key: str, default: Any) -> Any:
    with _lock:
        return _settings_cache.get(plugin_id, {}).get(key, default)

def set_setting(plugin_id: str, key: str, value: Any):
    with _lock:
        if plugin_id not in _settings_cache:
            _settings_cache[plugin_id] = {}
        _settings_cache[plugin_id][key] = value
        _save_settings_to_file()

def clear_settings(plugin_id: str):
    with _lock:
        if plugin_id in _settings_cache:
            del _settings_cache[plugin_id]
            _save_settings_to_file()

def get_all_settings(plugin_id: str) -> Any:
    with _lock:
        return _settings_cache.get(plugin_id, {})
        
def set_all_settings(plugin_id: str, settings: dict):
    with _lock:
        _settings_cache[plugin_id] = settings
        _save_settings_to_file()