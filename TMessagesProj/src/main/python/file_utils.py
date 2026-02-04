import os
import traceback
from typing import List, Optional
from java.io import File
from org.telegram.messenger import FileLoader
from tw.nekomimi.nekogram.plugins import PluginsController
from android_utils import log

def get_plugins_dir() -> str:
    return PluginsController.getInstance().pluginsDir.getAbsolutePath()

def get_cache_dir() -> str:
    return FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE).getAbsolutePath()

def get_files_dir() -> str:
    return FileLoader.getDirectory(FileLoader.MEDIA_DIR_FILES).getAbsolutePath()

def get_images_dir() -> str:
    return FileLoader.getDirectory(FileLoader.MEDIA_DIR_IMAGE).getAbsolutePath()

def get_videos_dir() -> str:
    return FileLoader.getDirectory(FileLoader.MEDIA_DIR_VIDEO).getAbsolutePath()

def get_audios_dir() -> str:
    return FileLoader.getDirectory(FileLoader.MEDIA_DIR_AUDIO).getAbsolutePath()

def get_documents_dir() -> str:
    return FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT).getAbsolutePath()

def read_file(file_path: str) -> Optional[str]:
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()
    except Exception as e:
        log(f'Error reading file {file_path}: {e}\n{traceback.format_exc()}')
        return None

def write_file(file_path: str, content: str):
    try:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
    except Exception as e:
        log(f'Error writing to file {file_path}: {e}\n{traceback.format_exc()}')

def delete_file(file_path: str) -> bool:
    if os.path.exists(file_path):
        try:
            os.remove(file_path)
        except OSError as e:
            log(f'Error deleting file {file_path}: {e}\n{traceback.format_exc()}')
            return False
        else:
            return True
    else:
        return False

def ensure_dir_exists(dir_path: str):
    try:
        os.makedirs(dir_path, exist_ok=True)
    except OSError as e:
        log(f'Error creating directory {dir_path}: {e}\n{traceback.format_exc()}')
        
def list_dir(path: str, recursive: bool=False, include_files: bool=True, include_dirs: bool=False, extensions: Optional[List[str]]=None) -> List[str]:
    results = []
    if not os.path.isdir(path):
        log(f'Path is not a directory or does not exist: {path}')
        return results
    else:
        if recursive:
            for root, dirs, files in os.walk(path):
                if include_dirs:
                    for d in dirs:
                        results.append(os.path.join(root, d))
                if include_files:
                    for f in files:
                        if not extensions or any((f.endswith(ext) for ext in extensions)):
                            results.append(os.path.join(root, f))
        else:
            for item in os.listdir(path):
                full_path = os.path.join(path, item)
                is_dir = os.path.isdir(full_path)
                if is_dir and include_dirs or (not is_dir and include_files):
                    if not is_dir and extensions and (not any((item.endswith(ext) for ext in extensions))):
                                continue
                    results.append(full_path)
        return results