import os
import traceback
from typing import Any, Optional, Callable
from java.io import File
from java.lang import Object
from java.util import ArrayList, HashMap, List
from android.graphics import Bitmap, BitmapFactory
from android.media import MediaMetadataRetriever
from android.webkit import MimeTypeMap
from org.telegram.messenger import AccountInstance, AndroidUtilities, DispatchQueue, ImageLoader, MediaController, MessageObject, NotificationCenter, R, SendMessagesHelper, UserConfig, Utilities
from org.telegram.tgnet import RequestDelegate, TLObject, TLRPC
from org.telegram.ui import LaunchActivity
from android_utils import R as Runnable
from android_utils import log, run_on_ui_thread
from java import Override, cast, dynamic_proxy, jarray, jbyte, jint, jvoid, static_proxy

STAGE_QUEUE = 'stageQueue'
GLOBAL_QUEUE = 'globalQueue'
CACHE_CLEAR_QUEUE = 'cacheClearQueue'
SEARCH_QUEUE = 'searchQueue'
PHONE_BOOK_QUEUE = 'phoneBookQueue'
THEME_QUEUE = 'themeQueue'
EXTERNAL_NETWORK_QUEUE = 'externalNetworkQueue'
PLUGINS_QUEUE = 'pluginsQueue'

def get_queue_by_name(queue_name: str) -> Optional[DispatchQueue]:
    try:
        field = Utilities.getClass().getField(queue_name)
        field.setAccessible(True)
        queue_instance = field.get(None)
        return queue_instance if isinstance(queue_instance, DispatchQueue) else None
    except Exception as e:
        log(f'Error getting queue \'{queue_name}\': {e}\n{traceback.format_exc()}')
        return None

def run_on_queue(fn: callable, queue_name: str=PLUGINS_QUEUE, delay: int=0):
    queue = get_queue_by_name(queue_name)
    if queue:
        try:
            queue.postRunnable(Runnable(fn), delay)
        except Exception as e:
            log(f'Error posting runnable to queue \'{queue_name}\': {e}\n{traceback.format_exc()}')

class RequestCallback(dynamic_proxy(RequestDelegate)):
    def __init__(self, fn: callable):
        super().__init__()
        self._fn = fn
    def run(self, response: TLObject, error: TLRPC.TL_error):
        try:
            self._fn(response, error)
        except Exception as e:
            log(f'Error in RequestDelegate: {e}\n{traceback.format_exc()}')

def send_request(request: Any, fn: callable) -> int:
    return get_connections_manager().sendRequest(request, RequestCallback(fn))

def get_last_fragment():
    return LaunchActivity.getSafeLastFragment()

def get_account_instance():
    return AccountInstance.getInstance(UserConfig.selectedAccount)

def get_messages_controller():
    return get_account_instance().getMessagesController()

def get_contacts_controller():
    return get_account_instance().getContactsController()

def get_media_data_controller():
    return get_account_instance().getMediaDataController()

def get_connections_manager():
    return get_account_instance().getConnectionsManager()

def get_location_controller():
    return get_account_instance().getLocationController()

def get_notifications_controller():
    return get_account_instance().getNotificationsController()

def get_messages_storage():
    return get_account_instance().getMessagesStorage()

def get_send_messages_helper():
    return get_account_instance().getSendMessagesHelper()

def get_file_loader():
    return get_account_instance().getFileLoader()

def get_secret_chat_helper():
    return get_account_instance().getSecretChatHelper()

def get_download_controller():
    return get_account_instance().getDownloadController()

def get_notifications_settings():
    return get_account_instance().getNotificationsSettings()

def get_notification_center():
    return get_account_instance().getNotificationCenter()

def get_media_controller():
    return MediaController.getInstance()

def get_user_config():
    return get_account_instance().getUserConfig()

def send_message(params: dict):
    send_messages_helper = get_send_messages_helper()
    message_params = send_messages_helper.SendMessageParams()
    defaults = {'peer': 0, 'message': None, 'photo': None, 'document': None, 'videoEditedInfo': None, 'path': None, 'replyToMsg': None, 'replyToTopMsg': None, 'entities': None, 'replyMarkup': None, 'params': None, 'searchLinks': True, 'notify': True, 'updateStickersOrder': False, 'hasMediaSpoilers': False, 'invert_media': False, 'sendingHighQuality': False, 'scheduleDate': 0, 'scheduleRepeatPeriod': 0, 'ttl': 0, 'effect_id': 0, 'stars': 0, 'payStars': 0, 'monoForumPeer': None, 'quick_reply_shortcut_id': None, 'quick_reply_shortcut': None, 'parentObject': None, 'replyToStoryItem': None, 'sendingStory': None, 'replyQuote': None, 'suggestionParams': None}
    final_params = defaults.copy()
    final_params.update(params)

    def cast_to_array_list(py_list):
        if not py_list:
            return None
        else:
            java_list = ArrayList(len(py_list))
            for item in py_list:
                java_list.add(item)
            return java_list
    for key, value in final_params.items():
        try:
            if key == 'entities' and value is not None:
                setattr(message_params, key, cast_to_array_list(value))
            else:
                setattr(message_params, key, value)
        except Exception as e:
            log(f'Could not set parameter \'{key}\': {e}')
    run_on_ui_thread(lambda: send_messages_helper.sendMessage(message_params))

def _generate_photo_sizes(file_path: str, high_quality: bool=False) -> Optional[TLRPC.TL_photo]:
    try: 
        photo_size_limit = AndroidUtilities.getPhotoSize(high_quality)
        bitmap = ImageLoader.loadBitmap(file_path, None, photo_size_limit, photo_size_limit, True)
        
        if not bitmap:
            log(f'Failed to load bitmap from {file_path}')
            return None
            
        sizes = ArrayList()
        thumb_size = ImageLoader.scaleAndSaveImage(bitmap, 90, 90, 55, True)
        if thumb_size:
            sizes.add(thumb_size)
            
        if high_quality:
            main_size = ImageLoader.scaleAndSaveImage(None, bitmap, Bitmap.CompressFormat.JPEG, True, photo_size_limit, photo_size_limit, 99, False, 101, 101, False)
        else:
            main_size = ImageLoader.scaleAndSaveImage(bitmap, photo_size_limit, photo_size_limit, True, 80, False, 101, 101)
            
        if main_size:
            sizes.add(main_size)
            
        bitmap.recycle()
        
        if sizes.isEmpty():
            return None
            
        photo = TLRPC.TL_photo()
        photo.date = get_connections_manager().getCurrentTime()
        photo.sizes = sizes
        photo.file_reference = jarray(jbyte)(0) 
        return photo
        
    except Exception as e:
        log(f'Error generating photo sizes for \'{file_path}\': {e}\n{traceback.format_exc()}')
        return None

def _prepare_document(file_path: str, mime_type_str: Optional[str]=None) -> Optional[TLRPC.TL_document]:
    try:
        file = File(file_path)
        if not file.exists():
            log(f'File not found: {file_path}')
            return
        else:
            file_name = file.getName()
            if not mime_type_str:
                extension = ''
                dot_index = file_name.rfind('.')
                if dot_index!= (-1):
                    extension = file_name[dot_index + 1:]
                mime_type_str = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lower())
                if not mime_type_str:
                    mime_type_str = 'application/octet-stream'
            doc = TLRPC.TL_document()
            doc.id = 0
            doc.access_hash = 0
            doc.file_reference = jarray(jbyte)(0)
            doc.date = get_connections_manager().getCurrentTime()
            doc.mime_type = mime_type_str
            doc.size = file.length()
            doc.dc_id = 0
            attr_file_name = TLRPC.TL_documentAttributeFilename()
            attr_file_name.file_name = file_name
            doc.attributes = ArrayList()
            doc.attributes.add(attr_file_name)
            doc.thumbs = ArrayList()
            return doc
    except Exception as e:
        log(f'Error preparing document for \'{file_path}\': {e}')
        return None

def _add_video_attributes(document: TLRPC.TL_document, file_path: str):
    try:
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(file_path)
        attr_video = TLRPC.TL_documentAttributeVideo()
        attr_video.w = int(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        attr_video.h = int(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        attr_video.duration = int(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000.0
        attr_video.supports_streaming = True
        document.attributes.add(attr_video)
        thumb_bitmap = retriever.getFrameAtTime()
        if thumb_bitmap:
            thumb_size = ImageLoader.scaleAndSaveImage(thumb_bitmap, 320, 320, 80, False)
            if thumb_size:
                document.thumbs.add(thumb_size)
            thumb_bitmap.recycle()
        retriever.release()
    except Exception as e:
        log(f'Could not get video metadata for \'{file_path}\': {e}')

def _add_audio_attributes(document: TLRPC.TL_document, file_path: str):
    try:
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(file_path)
        attr_audio = TLRPC.TL_documentAttributeAudio()
        attr_audio.duration = int(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000.0
        attr_audio.title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) or ''
        attr_audio.performer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) or ''
        document.attributes.add(attr_audio)
        retriever.release()
    except Exception as e:
        log(f'Could not get audio metadata for \'{file_path}\': {e}')

def send_text(peer: int, text: str, **kwargs):
    params = {'peer': peer, 'message': text}
    params.update(kwargs)
    send_message(params)

def send_photo(peer: int, file_path: str, caption: str='', high_quality: bool=False, **kwargs):
    photo = _generate_photo_sizes(file_path, high_quality)
    if not photo:
        log(f'Failed to create photo object for {file_path}')
        return None
    else:
        params = {'peer': peer, 'photo': photo, 'path': file_path, 'caption': caption, 'sendingHighQuality': high_quality}
        params.update(kwargs)
        send_message(params)

def send_document(peer: int, file_path: str, caption: str='', **kwargs):
    document = _prepare_document(file_path)
    if not document:
        log(f'Failed to prepare document for {file_path}')
        return None
    else:
        params = {'peer': peer, 'document': document, 'path': file_path, 'caption': caption}
        params.update(kwargs)
        send_message(params)

def send_video(peer: int, file_path: str, caption: str='', **kwargs):
    document = _prepare_document(file_path, 'video/mp4')
    if not document:
        log(f'Failed to prepare video document for {file_path}')
        return None
    else:
        _add_video_attributes(document, file_path)
        params = {'peer': peer, 'document': document, 'path': file_path, 'caption': caption}
        params.update(kwargs)
        send_message(params)

def send_audio(peer: int, file_path: str, caption: str='', **kwargs):
    document = _prepare_document(file_path, 'audio/mpeg')
    if not document:
        log(f'Failed to prepare audio document for {file_path}')
        return None
    else:
        _add_audio_attributes(document, file_path)
        params = {'peer': peer, 'document': document, 'path': file_path, 'caption': caption}
        params.update(kwargs)
        send_message(params)

def edit_message(message_obj: Any, text: Optional[str]=None, file_path: Optional[str]=None, with_spoiler: bool=False, **kwargs):
    if not isinstance(message_obj, MessageObject):
        log('\'message_obj\' must be a valid MessageObject instance.')
        return None
    else:
        if text is None and file_path is None:
            log('edit_message called with no changes (text or file_path). Cancelled.')
            return None
        else:
            if file_path and (not os.path.exists(file_path)):
                log(f'Media file not found at path: {file_path}')
                return None
            else:
                send_helper = get_send_messages_helper()
                if text is not None:
                    message_obj.editingMessage = text
                    message_obj.editingMessageEntities = None
                photo = None
                document = None
                video_info = None
                cover = None
                if file_path:
                    ext = os.path.splitext(file_path)[1].lower()
                    if ext in ['.jpg', '.jpeg', '.png', '.webp']:
                        photo = _generate_photo_sizes(file_path)
                        if not photo:
                            log(f'Failed to generate photo sizes for: {file_path}')
                            return
                    else:
                        if ext in ['.mp4', '.mov', '.mkv']:
                            document = _prepare_document(file_path, 'video/mp4')
                            if not document:
                                log(f'Failed to prepare document for video: {file_path}')
                                return None
                            else:
                                _add_video_attributes(document, file_path)
                        else:
                            if ext in ['.mp3', '.m4a', '.ogg', '.opus', '.flac']:
                                document = _prepare_document(file_path)
                                if not document:
                                    log(f'Failed to prepare document for audio: {file_path}')
                                    return None
                                else:
                                    _add_audio_attributes(document, file_path)
                            else:
                                document = _prepare_document(file_path)
                                if not document:
                                    log(f'Failed to prepare document for file: {file_path}')
                                    return None
                java_params = HashMap()
                for key, value in kwargs.items():
                    java_params.put(str(key), str(value))
                try:
                    run_on_ui_thread(lambda: send_helper.editMessage(message_obj, photo, video_info, document, file_path, cover, java_params, False, with_spoiler, None))
                    log(f'Edit request sent for message {message_obj.getId()} in dialog {message_obj.getDialogId()}.')
                except Exception as e:
                    log(f'An error occurred while calling editMessage: {e}\n{traceback.format_exc()}')

class NotificationCenterDelegate(static_proxy(None, NotificationCenter.NotificationCenterDelegate)):
    def __init__(self):
        super().__init__()

    @Override(jvoid, [jint, jint, jarray(Object)])
    def didReceivedNotification(self, id: int, account: int, args: jarray(Object)):
        return None