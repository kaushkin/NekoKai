from dataclasses import dataclass, field
from typing import List, Callable, Any
from android.view import View
from tw.nekomimi.nekogram.plugins import PluginsConstants

@dataclass
class Switch:
    key: str
    text: str
    default: bool
    subtext: str = None
    icon: str = None
    on_change: Callable[[bool], None] = field(default=None, compare=False, repr=False)
    type: str = field(default=PluginsConstants.Settings.TYPE_SWITCH, init=False)
    on_long_click: Callable[[View], None] = field(default=None, compare=False, repr=False)
    link_alias: str = None

@dataclass
class Selector:
    key: str
    text: str
    default: int
    items: List[str]
    icon: str = None
    on_change: Callable[[int], None] = field(default=None, compare=False, repr=False)
    type: str = field(default=PluginsConstants.Settings.TYPE_SELECTOR, init=False)
    on_long_click: Callable[[View], None] = field(default=None, compare=False, repr=False)
    link_alias: str = None

@dataclass
class Input:
    key: str
    text: str
    default: str = ''
    subtext: str = None
    icon: str = None
    on_change: Callable[[str], None] = field(default=None, compare=False, repr=False)
    type: str = field(default=PluginsConstants.Settings.TYPE_INPUT, init=False)
    on_long_click: Callable[[View], None] = field(default=None, compare=False, repr=False)
    link_alias: str = None

@dataclass
class Text:
    text: str
    icon: str = None
    accent: bool = False
    red: bool = False
    on_click: Callable[[View], None] = field(default=None, compare=False, repr=False)
    create_sub_fragment: Callable[[], List[Any]] = field(default=None, compare=False, repr=False)
    type: str = field(default=PluginsConstants.Settings.TYPE_TEXT, init=False)
    on_long_click: Callable[[View], None] = field(default=None, compare=False, repr=False)
    link_alias: str = None

@dataclass
class Header:
    text: str
    type: str = field(default=PluginsConstants.Settings.TYPE_HEADER, init=False)

@dataclass
class Divider:
    text: str = None
    type: str = field(default=PluginsConstants.Settings.TYPE_DIVIDER, init=False)
    
@dataclass
class EditText:
    key: str
    hint: str
    default: str = ''
    multiline: bool = False
    max_length: int = 256
    mask: str = None
    on_change: Callable[[str], None] = field(default=None, compare=False, repr=False)
    type: str = field(default=PluginsConstants.Settings.TYPE_EDIT_TEXT, init=False)