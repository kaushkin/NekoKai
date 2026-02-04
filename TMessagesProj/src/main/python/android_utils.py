import traceback
from typing import Any
from android.view import View
from java import dynamic_proxy, static_proxy, Override, jvoid, jclass
from java.lang import Runnable
from org.telegram.messenger import AndroidUtilities
from tw.nekomimi.nekogram.utils import AppUtils

class OnClickListener(dynamic_proxy(View.OnClickListener)):
    def __init__(self, fn: callable):
        super().__init__()
        self._fn = fn

    def onClick(self, _view):
        try:
            self._fn(_view)
        except Exception as e:
            log(f'Error in onClick listener: {e}\n{traceback.format_exc()}')

class OnLongClickListener(dynamic_proxy(View.OnLongClickListener)):
    def __init__(self, fn: callable):
        super().__init__()
        self._fn = fn

    def onLongClick(self, _view):
        try:
            self._fn(_view)
        except Exception as e:
            log(f'Error in onLongClick listener: {e}\n{traceback.format_exc()}')
            return False
        else:
            return True

def run_on_ui_thread(func: callable, delay=0):
    AndroidUtilities.runOnUIThread(R(func), delay)

def log(data: Any):
    if isinstance(data, (str, int, float, bool)) or data is None:
        AppUtils.log(str(data))
    else:
        AppUtils.printObjectDetails(data)

def copy_to_clipboard(text: str):
    if AndroidUtilities.addToClipboard(text):
        from ui.bulletin import BulletinHelper
        BulletinHelper.show_copied_to_clipboard()
        
class R(static_proxy(None, Runnable)):
    def __init__(self, fn):
        super(R, self).__init__()
        self._fn = fn
    @Override(jvoid, [])
    def run(self):
        try:
            self._fn()
        except Exception as e:
            log(f'Error in runnable: {e}\n{traceback.format_exc()}')