from typing import Any, Optional
import traceback
from java import jclass, jarray
from android_utils import log

JavaClass = type(jclass('java.lang.Object'))
JavaObject = jclass('java.lang.Object')

def find_class(class_name: str) -> Optional[JavaClass]:
    if not isinstance(class_name, str):
        return None
    
    try:
        clazz = jclass(class_name)
        log(f'Successfully found class: {class_name}')
        return clazz
    except Exception as e:
        log(f'Error finding class \'{class_name}\': {e}\n{traceback.format_exc()}')
        return None

def get_private_field(obj: JavaObject, field_name: str) -> Optional[Any]:
    if not isinstance(obj, JavaObject) or not isinstance(field_name, str):
        return None
        
    clazz = obj.getClass()
    field = None
    current_class = clazz
    
    while current_class is not None:
        try:
            field = current_class.getDeclaredField(field_name)
        except Exception:
            current_class = current_class.getSuperclass()
        else:
            break
            
    if field is None:
        return None

    try:
        field.setAccessible(True)
        value = field.get(obj)
        log(f'Successfully accessed private field \'{field_name}\' on object {obj}')
        return value
    except Exception as e:
        log(f'Error accessing field \'{field_name}\' on {obj}: {e}\n{traceback.format_exc()}')
        return None

def set_private_field(obj: JavaObject, field_name: str, new_value: Any) -> bool:
    if not isinstance(obj, JavaObject) or not isinstance(field_name, str):
        return False
        
    clazz = obj.getClass()
    field = None
    current_class = clazz
    
    while current_class is not None:
        try:
            field = current_class.getDeclaredField(field_name)
        except Exception:
            current_class = current_class.getSuperclass()
        else:
            break
            
    if field is None:
        return False

    try:
        field.setAccessible(True)
        field.set(obj, new_value)
        log(f'Successfully set private field \'{field_name}\' on object {obj}')
        return True
    except Exception as e:
        log(f'Error setting field \'{field_name}\' on {obj}: {e}\n{traceback.format_exc()}')
        return False

def get_static_private_field(clazz: JavaClass, field_name: str) -> Optional[Any]:
    if type(clazz) is not JavaClass or not isinstance(field_name, str):
        return None
        
    field = None
    current_class = clazz
    
    while current_class is not None:
        try:
            field = current_class.getDeclaredField(field_name)
        except Exception:
            current_class = current_class.getSuperclass()
        else:
            break
            
    if field is None:
        return None

    try:
        field.setAccessible(True)
        value = field.get(None) 
        log(f'Successfully accessed static private field \'{field_name}\' from class {clazz.getName()}')
        return value
    except Exception as e:
        log(f'Error accessing static field \'{field_name}\' from {clazz.getName()}: {e}\n{traceback.format_exc()}')
        return None

def set_static_private_field(clazz: JavaClass, field_name: str, new_value: Any) -> bool:
    if type(clazz) is not JavaClass or not isinstance(field_name, str):
        return False
        
    field = None
    current_class = clazz
    
    while current_class is not None:
        try:
            field = current_class.getDeclaredField(field_name)
        except Exception:
            current_class = current_class.getSuperclass()
        else:
            break
            
    if field is None:
        log(f'Field \'{field_name}\' not found in class hierarchy of {clazz.getName()}')
        return False

    try:
        field.setAccessible(True)
        field.set(None, new_value)
        log(f'Successfully set static private field \'{field_name}\' on class {clazz.getName()}')
        return True
    except Exception as e:
        log(f'Error setting static field \'{field_name}\' on {clazz.getName()}: {e}\n{traceback.format_exc()}')
        return False