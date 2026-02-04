import traceback
from abc import ABC, abstractmethod
from dataclasses import dataclass
from de.robv.android.xposed import XposedBridge, XC_MethodHook
from enum import Enum
from java.lang import Class
from java.lang.reflect import Member
from java.util import ArrayList
from typing import Any, List, final, Optional, Callable, Dict, Union
import plugin_settings
from android_utils import log
from tw.nekomimi.nekogram import NekoConfig
from tw.nekomimi.nekogram.plugins import PluginsController, PluginsConstants
from tw.nekomimi.nekogram.plugins.hooks import HookFilter as JavaHookFilter
from tw.nekomimi.nekogram.plugins.xposed import PyMethodHook, PyMethodReplacement

@dataclass
class PluginMetadata:
    id: str
    name: str
    description: str
    author: str
    version: str
    icon: str
    min_version: str

class HookStrategy(Enum):
    CANCEL = PluginsConstants.Strategy.CANCEL
    MODIFY = PluginsConstants.Strategy.MODIFY
    DEFAULT = PluginsConstants.Strategy.DEFAULT
    MODIFY_FINAL = PluginsConstants.Strategy.MODIFY_FINAL

@dataclass
class HookResult:
    strategy: HookStrategy = HookStrategy.DEFAULT
    request: Any = None
    response: Any = None
    update: Any = None
    updates: Any = None
    error: Any = None
    params: Any = None

class PluginError(Exception):
    plugin_id: Optional[str]
    def __init__(self, message: str, plugin_id: Optional[str]=None):
        super().__init__(message)
        self.plugin_id = plugin_id

class XposedHook(ABC):
    pass

class MethodReplacement(XposedHook):
    @abstractmethod
    def replace_hooked_method(self, param: XC_MethodHook.MethodHookParam) -> Any:
        return

class MethodHook(XposedHook):
    def before_hooked_method(self, param: XC_MethodHook.MethodHookParam):
        return

    def after_hooked_method(self, param: XC_MethodHook.MethodHookParam):
        return

class AppEvent(Enum):
    START = PluginsConstants.APP_START
    STOP = PluginsConstants.APP_STOP
    PAUSE = PluginsConstants.APP_PAUSE
    RESUME = PluginsConstants.APP_RESUME

class MenuItemType(Enum):
    MESSAGE_CONTEXT_MENU = PluginsConstants.MenuItemTypes.MESSAGE_CONTEXT_MENU
    DRAWER_MENU = PluginsConstants.MenuItemTypes.DRAWER_MENU
    CHAT_ACTION_MENU = PluginsConstants.MenuItemTypes.CHAT_ACTION_MENU
    PROFILE_ACTION_MENU = PluginsConstants.MenuItemTypes.PROFILE_ACTION_MENU

@dataclass
class MenuItemData:
    menu_type: MenuItemType
    text: str
    on_click: Callable[[Dict[str, Any]], None]
    item_id: Optional[str] = None
    icon: Optional[str] = None
    subtext: Optional[str] = None
    condition: Optional[str] = None
    priority: int = 0

@dataclass
class HookFilterData:
    filter_type: str
    arg_index: Optional[int] = None
    or_filters: Optional[Any] = None
    mvel_expression: Optional[str] = None
    instance_of: Optional[Any] = None
    object: Any = None

    def to_java_filter(self) -> JavaHookFilter:
        if not self.filter_type:
            return
        else:
            filter = JavaHookFilter(self.filter_type)
            if self.arg_index is not None and isinstance(self.arg_index, int):
                    filter.argIndex = self.arg_index
            if self.or_filters is not None and isinstance(self.or_filters, ArrayList):
                    filter.orFilters = self.or_filters
            if self.mvel_expression is not None and isinstance(self.mvel_expression, str):
                    filter.mvelExpression = self.mvel_expression
            if self.instance_of is not None:
                filter.instanceOf = self.instance_of
            if self.object is not None:
                filter.object = self.object
            return filter

class HookFilter(Enum):
    RESULT_IS_NULL = PluginsConstants.HookFilterTypes.RESULT_IS_NULL
    RESULT_IS_TRUE = PluginsConstants.HookFilterTypes.RESULT_IS_TRUE
    RESULT_IS_FALSE = PluginsConstants.HookFilterTypes.RESULT_IS_FALSE
    RESULT_NOT_NULL = PluginsConstants.HookFilterTypes.RESULT_NOT_NULL

    @property
    def filter_data(self):
        return HookFilterData(filter_type=self.value)

    @staticmethod
    def ResultIsInstanceOf(clazz):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.RESULT_IS_INSTANCE_OF, instance_of=clazz)
   
    @staticmethod
    def ResultEqual(value):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.RESULT_EQUAL, object=value)
   
    @staticmethod
    def ResultNotEqual(value):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.RESULT_NOT_EQUAL, object=value)
   
    @staticmethod
    def ArgumentIsNull(index: int):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.ARGUMENT_IS_NULL, arg_index=index)
   
    @staticmethod
    def ArgumentIsTrue(index: int):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.ARGUMENT_IS_TRUE, arg_index=index)
 
    @staticmethod
    def ArgumentIsFalse(index: int):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.ARGUMENT_IS_FALSE, arg_index=index)
  
    @staticmethod
    def ArgumentNotNull(index: int):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.ARGUMENT_NOT_NULL, arg_index=index)
  
    @staticmethod
    def ArgumentIsInstanceOf(index: int, clazz):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.ARGUMENT_IS_INSTANCE_OF, arg_index=index, instance_of=clazz)
  
    @staticmethod
    def ArgumentEqual(index: int, value):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.ARGUMENT_EQUAL, arg_index=index, object=value)
  
    @staticmethod
    def ArgumentNotEqual(index: int, value):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.ARGUMENT_NOT_EQUAL, arg_index=index, object=value)
  
    @staticmethod
    def Condition(condition: str, object: Any=None):
        return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.CONDITION, mvel_expression=condition, object=object)
  
    @staticmethod
    def Or(*filters):
        if not filters:
            return
        else:
            arr = ArrayList(len(filters))
            for item in filters:
                if not isinstance(item, (HookFilter, HookFilterData)):
                    continue
                else:
                    arr.add((item if isinstance(item, HookFilterData) else item.filter_data).to_java_filter())
            return HookFilterData(filter_type=PluginsConstants.HookFilterTypes.OR, or_filters=arr)

def hook_filters(*filters):
    def decorator(func):
        target = []
        if filters:
            if isinstance(filters[0], list):
                target = filters[0]
            else:
                target = filters
        func.__hook_filters__ = [
            (f if isinstance(f, HookFilterData) else f.filter_data).to_java_filter() 
            for f in target
        ]
        return func
    return decorator

class BasePlugin:
    def __init__(self):
        self.id = ''
        self.name = ''
        self.description = ''
        self.author = ''
        self.min_version = ''
        self.version = ''
        self.icon = None
        self.error_message = None
        self.enabled = False
        self.initialized = False
  
    def on_plugin_load(self):
        return
  
    def on_plugin_unload(self):
        return
  
    def create_settings(self) -> List[Any]:
        return []
  
    def on_app_event(self, event_type: AppEvent):
        return
  
    def pre_request_hook(self, request_name: str, account: int, request: Any) -> HookResult:
        return HookResult()
  
    def post_request_hook(self, request_name: str, account: int, response: Any, error: Any) -> HookResult:
        return HookResult()
  
    def on_update_hook(self, update_name: str, account: int, update: Any) -> HookResult:
        return HookResult()
  
    def on_updates_hook(self, container_name: str, account: int, updates: Any) -> HookResult:
        return HookResult()
  
    def on_send_message_hook(self, account: int, params: Any) -> HookResult:
        return HookResult()
  
    @final
    def add_hook(self, name: str, match_substring: bool=False, priority: int=0):
        if not self.id or not name or self.error_message:
            return None
        else:
            PluginsController.getInstance().addEventHook(self.id, name, match_substring, priority)
 
    @final
    def add_on_send_message_hook(self, priority: int=0):
        self.add_hook(PluginsConstants.SEND_MESSAGE_HOOK, False, priority)
 
    @final
    def remove_hook(self, name: str):
        if not self.id or not name:
            return None
        else:
            PluginsController.getInstance().removeEventHook(self.id, name)
  
    @final
    def get_setting(self, key: str, default: Any=None) -> Any:
        if not self.id or not key or self.error_message:
            return default
        else:
            return plugin_settings.get_setting(self.id, key, default)
  
    @final
    def set_setting(self, key: str, value: Any, reload_settings: bool=False):
        if not self.id or not key or self.error_message:
            return None
        else:
            plugin_settings.set_setting(self.id, key, value)
            if reload_settings:
                PluginsController.getInstance().loadPluginSettings(self.id)
 
    @final
    def export_settings(self) -> dict:
        if not self.id or self.error_message:
            return {}
        else:
            return plugin_settings.get_all_settings(self.id)
   
    @final
    def import_settings(self, settings: dict, reload_settings: bool=True):
        if not self.id or self.error_message:
            return None
        else:
            plugin_settings.set_all_settings(self.id, settings)
            if reload_settings:
                PluginsController.getInstance().loadPluginSettings(self.id)
  
    @final
    def __get_java_callback_instance(self, xposed_hook: Optional[Any], priority: Optional[int]=None):
        if xposed_hook is None:
            raise TypeError('xposed_hook cannot be None')
        else:
            if NekoConfig.pluginsSafeMode:
                return
            else:
                def cast_to_array_list(value):
                    if isinstance(value, Callable):
                        value = value(xposed_hook)
                    if not value:
                        return ArrayList()
                    else:
                        arr = ArrayList(len(value))
                        for item in value:
                            arr.add(item)
                        return arr
                java_callback_instance = None
                if isinstance(xposed_hook, MethodReplacement):
                    if priority is not None:
                        java_callback_instance = PyMethodReplacement(self.id, xposed_hook, priority)
                    else:
                        java_callback_instance = PyMethodReplacement(self.id, xposed_hook)
                else:
                    if isinstance(xposed_hook, MethodHook):
                        if priority is not None:
                            java_callback_instance = PyMethodHook(self.id, xposed_hook, priority)
                        else:
                            java_callback_instance = PyMethodHook(self.id, xposed_hook)
                        if hasattr((method := getattr(xposed_hook, PluginsConstants.Xposed.BEFORE_HOOKED_METHOD, None)), PluginsConstants.Xposed.HOOK_FILTERS):
                            java_callback_instance.setBeforeHookedFilters(cast_to_array_list(getattr(method, PluginsConstants.Xposed.HOOK_FILTERS)))
                        if hasattr((method := getattr(xposed_hook, PluginsConstants.Xposed.AFTER_HOOKED_METHOD, None)), PluginsConstants.Xposed.HOOK_FILTERS):
                            java_callback_instance.setAfterHookedFilters(cast_to_array_list(getattr(method, PluginsConstants.Xposed.HOOK_FILTERS)))
                    else:
                        if hasattr(xposed_hook, PluginsConstants.Xposed.REPLACE_HOOKED_METHOD):
                            if priority is not None:
                                java_callback_instance = PyMethodReplacement(self.id, xposed_hook, priority)
                            else:
                                java_callback_instance = PyMethodReplacement(self.id, xposed_hook)
                        else:
                            if hasattr(xposed_hook, PluginsConstants.Xposed.BEFORE_HOOKED_METHOD) or hasattr(xposed_hook, PluginsConstants.Xposed.AFTER_HOOKED_METHOD):
                                if priority is not None:
                                    java_callback_instance = PyMethodHook(self.id, xposed_hook, priority)
                                else:
                                    java_callback_instance = PyMethodHook(self.id, xposed_hook)
                                if hasattr((method := getattr(xposed_hook, PluginsConstants.Xposed.BEFORE_HOOKED_METHOD, None)), PluginsConstants.Xposed.HOOK_FILTERS):
                                    java_callback_instance.setBeforeHookedFilters(cast_to_array_list(getattr(method, PluginsConstants.Xposed.HOOK_FILTERS)))
                                if hasattr((method := getattr(xposed_hook, PluginsConstants.Xposed.AFTER_HOOKED_METHOD, None)), PluginsConstants.Xposed.HOOK_FILTERS):
                                    java_callback_instance.setAfterHookedFilters(cast_to_array_list(getattr(method, PluginsConstants.Xposed.HOOK_FILTERS)))
                return java_callback_instance
   
    @final
    def hook_method(self, method_or_constructor: Member, xposed_hook: Optional[Any], priority: Optional[int]=None) -> Optional[XC_MethodHook.Unhook]:
        if xposed_hook is None:
            raise TypeError('xposed_hook cannot be None')
        if NekoConfig.pluginsSafeMode:
            return None
        unhook = None
        java_callback_instance = self.__get_java_callback_instance(xposed_hook, priority)
        if java_callback_instance is None:
            self.log(f'Could not determine hook type for {type(xposed_hook).__name__}.')
            return None
        try:
            unhook = XposedBridge.hookMethod(method_or_constructor, java_callback_instance)
            PluginsController.getInstance().addXposedHook(self.id, unhook)
            return unhook
        except Exception as e:
            self.log(f'Error while hooking method {method_or_constructor.getName()}: {e}')
            if unhook is not None:
                self.log('An error occurred after the hook was created, attempting to unhook it immediately.')
                unhook.unhook()
            return None
  
    @final
    def hook_all_methods(self, hook_class: Class, method_name: str, xposed_hook: Optional[Any], priority: Optional[int]=None) -> Optional[List[XC_MethodHook.Unhook]]:
        if xposed_hook is None or method_name is None:
            raise TypeError('xposed_hook and method_name cannot be None')
        if NekoConfig.pluginsSafeMode:
            return None
        java_callback_instance = self.__get_java_callback_instance(xposed_hook, priority)
        if java_callback_instance is None:
            self.log(f'Could not determine hook type for {type(xposed_hook).__name__}.')
            return None
        try:
            unhooks = XposedBridge.hookAllMethods(hook_class, method_name, java_callback_instance)
            if not unhooks:
                self.log(f'if not unhooks')
                return None
            PluginsController.getInstance().addXposedHooks(self.id, ArrayList(unhooks))
            self.log(f'hooks : {unhooks.toArray()}')
            return list(unhooks.toArray())
        except Exception as e:
            self.log(f'Error while hooking class methods with name {method_name} for {hook_class.getName()}: {e}')
            return None
  
    @final
    def hook_all_constructors(self, hook_class: Class, xposed_hook: Optional[Any], priority: Optional[int]=None) -> Optional[List[XC_MethodHook.Unhook]]:
        if xposed_hook is None:
            raise TypeError('xposed_hook cannot be None')
        if NekoConfig.pluginsSafeMode:
            return None
        java_callback_instance = self.__get_java_callback_instance(xposed_hook, priority)
        if java_callback_instance is None:
            self.log(f'Could not determine hook type for {type(xposed_hook).__name__}.')
            return None
        try:
            unhooks = XposedBridge.hookAllConstructors(hook_class, java_callback_instance)
            if not unhooks:
                return None
            PluginsController.getInstance().addXposedHooks(self.id, ArrayList(unhooks))
            return list(unhooks.toArray())
        except Exception as e:
            self.log(f'Error while hooking class constructors for {hook_class.getName()}: {e}')
            return None
   
    @final
    def unhook_method(self, unhook: XC_MethodHook.Unhook):
        PluginsController.getInstance().removeXposedHook(self.id, unhook)
  
    @final
    def log(self, message: str):
        log(f'[{self.id}] {message}')
  
    @final
    def add_menu_item(self, menu_item_data: MenuItemData) -> Optional[str]:
        if not self.id or self.error_message or (not menu_item_data):
            return None
        else:
            try:
                new_item_id = PluginsController.getInstance().addMenuItem(self.id, {PluginsConstants.MenuItemProperties.MENU_TYPE: menu_item_data.menu_type.value, PluginsConstants.MenuItemProperties.TEXT: menu_item_data.text, PluginsConstants.MenuItemProperties.ON_CLICK: menu_item_data.on_click, PluginsConstants.MenuItemProperties.ITEM_ID: menu_item_data.item_id, PluginsConstants.MenuItemProperties.ICON: menu_item_data.icon, PluginsConstants.MenuItemProperties.SUBTEXT: menu_item_data.subtext, PluginsConstants.MenuItemProperties.CONDITION: menu_item_data.condition, PluginsConstants.MenuItemProperties.PRIORITY: menu_item_data.priority})
                return new_item_id if new_item_id else None
            except Exception as e:
                self.log(f'Error adding menu item: {e}')
                return None
   
    @final
    def remove_menu_item(self, item_id: str) -> bool:
        if not self.id or not item_id or self.error_message:
            return False
        else:
            try:
                return PluginsController.getInstance().removeMenuItem(self.id, item_id)
            except Exception as e:
                self.log(f'Error removing menu item {item_id}: {e}')
                return False