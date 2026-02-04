import ast
import traceback

def get_metadata(file_path):
    metadata = {}
    allowed_keys = {'__version__', '__min_version__', '__id__', '__icon__', '__name__', '__description__', '__author__'}
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            source = f.read()
        tree = ast.parse(source, filename=file_path)
        for node in ast.iter_child_nodes(tree):
            if isinstance(node, ast.Assign):
                for target in node.targets:
                    if isinstance(target, ast.Name) and target.id in allowed_keys and isinstance(node.value, ast.Constant):
                                metadata[target.id[2:(-2)]] = node.value.value
    except Exception as e:
        raise Exception(f'Failed to parse metadata: {e}\n{traceback.format_exc()}')
    return metadata