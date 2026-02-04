
from typing import Callable, List, Optional, Dict
import traceback
from android.content import Context, DialogInterface
from android.graphics.drawable import Drawable
from android.view import View
from android.widget import TextView
from android_utils import log
from tw.nekomimi.nekogram import NekoConfig
from java import dynamic_proxy, Override, cast
from org.telegram.ui.ActionBar import AlertDialog, Theme

class _ButtonClickListenerProxy(dynamic_proxy(AlertDialog.OnButtonClickListener)):
    def __init__(self, py_callable: Callable[['AlertDialogBuilder', int], None], builder_instance: 'AlertDialogBuilder'):
        super().__init__()
        self._py_callable = py_callable
        self._builder_instance = builder_instance
    
    def onClick(self, dialog_java_instance: AlertDialog, which: int):
        try:
            if self._py_callable:
                self._py_callable(self._builder_instance, which)
        except Exception as e:
            log(f'Error in AlertDialog ButtonClickListener: {e}\n{traceback.format_exc()}')

class _ItemsClickListenerProxy(dynamic_proxy(DialogInterface.OnClickListener)):
    def __init__(self, py_callable: Callable[['AlertDialogBuilder', int], None], builder_instance: 'AlertDialogBuilder'):
        super().__init__()
        self._py_callable = py_callable
        self._builder_instance = builder_instance

    def onClick(self, dialog_java_instance: DialogInterface, which: int):
        try:
            if self._py_callable:
                self._py_callable(self._builder_instance, which)
        except Exception as e:
            log(f'Error in AlertDialog ItemsClickListener: {e}\n{traceback.format_exc()}')

class _DismissListenerProxy(dynamic_proxy(DialogInterface.OnDismissListener)):
    def __init__(self, py_callable: Callable[['AlertDialogBuilder'], None], builder_instance: 'AlertDialogBuilder'):
        super().__init__()
        self._py_callable = py_callable
        self._builder_instance = builder_instance

    def onDismiss(self, dialog_java_instance: DialogInterface):
        try:
            if self._py_callable:
                self._py_callable(self._builder_instance)
        except Exception as e:
            log(f'Error in AlertDialog DismissListener: {e}\n{traceback.format_exc()}')

class _CancelListenerProxy(dynamic_proxy(DialogInterface.OnCancelListener)):
    def __init__(self, py_callable: Callable[['AlertDialogBuilder'], None], builder_instance: 'AlertDialogBuilder'):
        super().__init__()
        self._py_callable = py_callable
        self._builder_instance = builder_instance

    def onCancel(self, dialog_java_instance: DialogInterface):
        try:
            if self._py_callable:
                self._py_callable(self._builder_instance)
        except Exception as e:
            log(f'Error in AlertDialog CancelListener: {e}\n{traceback.format_exc()}')

class AlertDialogBuilder:
    ALERT_TYPE_MESSAGE = 0
    ALERT_TYPE_LOADING = 2
    ALERT_TYPE_SPINNER = 3
    BUTTON_POSITIVE = (-1)
    BUTTON_NEGATIVE = (-2)
    BUTTON_NEUTRAL = (-3)

    def __init__(self, context: Context, progress_style: int=ALERT_TYPE_MESSAGE, resources_provider: Optional[Theme.ResourcesProvider]=None):
        self._context = context
        self._java_builder = AlertDialog.Builder(context, progress_style, resources_provider)
        self._alert_dialog = None
        self._red_buttons = []
        self._progress_style = progress_style

    def get_context(self) -> Context:
        """Returns the context used by this builder."""
        return self._context

    def set_title(self, title: str) -> 'AlertDialogBuilder':
        """Sets the title of the dialog."""
        self._java_builder.setTitle(title)
        return self

    def set_message(self, message: str) -> 'AlertDialogBuilder':
        """Sets the message content of the dialog."""
        self._java_builder.setMessage(message)
        return self

    def set_message_text_view_clickable(self, clickable: bool) -> 'AlertDialogBuilder':
        """Sets whether the message TextView is clickable (e.g., for links)."""
        self._java_builder.setMessageTextViewClickable(clickable)
        return self

    def set_positive_button(self, text: str, listener: Optional[Callable[['AlertDialogBuilder', int], None]]=None) -> 'AlertDialogBuilder':
        """Sets the positive button text and its click listener."""
        java_listener = _ButtonClickListenerProxy(listener, self) if listener else None
        self._java_builder.setPositiveButton(text, java_listener)
        return self

    def set_negative_button(self, text: str, listener: Optional[Callable[['AlertDialogBuilder', int], None]]=None) -> 'AlertDialogBuilder':
        """Sets the negative button text and its click listener."""
        java_listener = _ButtonClickListenerProxy(listener, self) if listener else None
        self._java_builder.setNegativeButton(text, java_listener)
        return self

    def set_neutral_button(self, text: str, listener: Optional[Callable[['AlertDialogBuilder', int], None]]=None) -> 'AlertDialogBuilder':
        """Sets the neutral button text and its click listener."""
        java_listener = _ButtonClickListenerProxy(listener, self) if listener else None
        self._java_builder.setNeutralButton(text, java_listener)
        return self

    def make_button_red(self, button_type: int) -> 'AlertDialogBuilder':
        """\n        Marks a button to be styled with red text after the dialog is shown.\n        Use `AlertDialogBuilder.BUTTON_POSITIVE`, `BUTTON_NEGATIVE`, or `BUTTON_NEUTRAL`.\n        """
        if button_type not in self._red_buttons:
            self._red_buttons.append(button_type)
        return self

    def set_on_back_button_listener(self, listener: Optional[Callable[['AlertDialogBuilder', int], None]]=None) -> 'AlertDialogBuilder':
        """Sets a listener for when the back button is pressed while the dialog is showing."""
        java_listener = _ButtonClickListenerProxy(listener, self) if listener else None
        self._java_builder.setOnBackButtonListener(java_listener)
        return self

    def set_view(self, view: View, height: int=(-2)) -> 'AlertDialogBuilder':
        """Sets a custom view for the dialog."""
        self._java_builder.setView(view, height)
        return self

    def set_items(self, items: List[str], listener: Optional[Callable[['AlertDialogBuilder', int], None]]=None, icons: Optional[List[int]]=None) -> 'AlertDialogBuilder':
        """Sets a list of items to be displayed in the dialog."""
        java_listener = _ItemsClickListenerProxy(listener, self) if listener else None
        if icons:
            self._java_builder.setItems(items, icons, java_listener)
        else:
            self._java_builder.setItems(items, java_listener)
        return self

    def set_on_dismiss_listener(self, listener: Optional[Callable[['AlertDialogBuilder'], None]]=None) -> 'AlertDialogBuilder':
        """Sets a listener to be invoked when the dialog is dismissed."""
        java_listener = _DismissListenerProxy(listener, self) if listener else None
        self._java_builder.setOnDismissListener(java_listener)
        return self

    def set_on_cancel_listener(self, listener: Optional[Callable[['AlertDialogBuilder'], None]]=None) -> 'AlertDialogBuilder':
        """Sets a listener to be invoked when the dialog is cancelled."""
        java_listener = _CancelListenerProxy(listener, self) if listener else None
        self._java_builder.setOnCancelListener(java_listener)
        return self

    def set_top_image(self, res_id: int, background_color: int) -> 'AlertDialogBuilder':
        """Sets an image resource at the top of the dialog."""
        self._java_builder.setTopImage(res_id, background_color)
        return self

    def set_top_drawable(self, drawable: Drawable, background_color: int) -> 'AlertDialogBuilder':
        """Sets a drawable at the top of the dialog."""
        self._java_builder.setTopImage(drawable, background_color)
        return self

    def set_top_animation(self, res_id: int, size: int, auto_repeat: bool, background_color: int, layer_colors: Optional[Dict[str, int]]=None) -> 'AlertDialogBuilder':
        """Sets an animation at the top of the dialog."""
        self._java_builder.setTopAnimation(res_id, size, auto_repeat, background_color, layer_colors)
        return self

    def set_top_animation_is_new(self, is_new: bool) -> 'AlertDialogBuilder':
        """Configures the style of the top animation area."""
        self._java_builder.setTopAnimationIsNew(is_new)
        return self

    def set_dim_enabled(self, enabled: bool) -> 'AlertDialogBuilder':
        """Enable or disable dimming of the background behind the dialog."""
        self._java_builder.setDimEnabled(enabled)
        return self

    def set_dialog_button_color_key(self, theme_key: int) -> 'AlertDialogBuilder':
        """Sets the theme color key for dialog buttons."""
        self._java_builder.setDialogButtonColorKey(theme_key)
        return self
        
    def set_blurred_background(self, blur: bool, blur_behind_if_possible: bool=True) -> 'AlertDialogBuilder':
        """\n        Tries to set a blurred background for the dialog.\n        Actual blurring depends on device capabilities.\n        """
        self._java_builder.setBlurredBackground(blur)
        if self._alert_dialog and blur:
                self._alert_dialog.setBlurParams(0.8, blur_behind_if_possible, blur)
        return self

    def create(self) -> 'AlertDialogBuilder':
        """Creates the AlertDialog but does not show it. Call show() to display."""
        if not self._alert_dialog:
            self._alert_dialog = self._java_builder.create()
            self._apply_red_buttons()
        return self

    def show(self) -> 'AlertDialogBuilder':
        """Creates and shows the AlertDialog."""
        if not self._alert_dialog:
            self._alert_dialog = self._java_builder.show()
            self._apply_red_buttons()
        else:
            if not self._alert_dialog.isShowing():
                self._alert_dialog.show()
                self._apply_red_buttons()
        return self

    def _apply_red_buttons(self):
        if self._alert_dialog:
            for button_type in self._red_buttons:
                button_view = self._alert_dialog.getButton(button_type)
                if button_view and isinstance(button_view, TextView):
                        text_view = cast(TextView, button_view)
                        text_view.setTextColor(self._alert_dialog.getThemedColor(Theme.key_text_RedBold))
    def dismiss(self):
        """Dismisses the dialog if it\'s showing."""
        if self._alert_dialog and self._alert_dialog.isShowing():
                self._alert_dialog.dismiss()

    def get_dialog(self) -> Optional[AlertDialog]:
        """Returns the underlying Java AlertDialog instance, if created."""
        return self._alert_dialog

    def get_button(self, button_type: int) -> Optional[View]:
        """\n        Gets a button view from the dialog.\n        Call this after create() or show().\n        Use `AlertDialogBuilder.BUTTON_POSITIVE`, etc.\n        """
        if self._alert_dialog:
            return self._alert_dialog.getButton(button_type)

    def set_progress(self, progress: int):
        """Sets the progress for ALERT_TYPE_LOADING style dialogs."""
        if self._alert_dialog and self._progress_style == AlertDialogBuilder.ALERT_TYPE_LOADING:
            self._alert_dialog.setProgress(progress)
        else:
            if not self._alert_dialog:
                log('AlertDialogBuilder: set_progress called before dialog is created.')
    
    def set_cancelable(self, cancelable: bool):
        """Sets whether the dialog is cancelable."""
        if self._alert_dialog:
            self._alert_dialog.setCancelable(cancelable)
        else:
            log('AlertDialogBuilder: set_cancelable is best called after create() or show().')
    
    def set_canceled_on_touch_outside(self, cancel: bool):
        """Sets whether the dialog is canceled when touched outside its window\'s bounds."""
        if self._alert_dialog:
            self._alert_dialog.setCanceledOnTouchOutside(cancel)
        else:
            log('AlertDialogBuilder: set_canceled_on_touch_outside is best called after create() or show().')