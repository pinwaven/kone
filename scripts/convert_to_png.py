from PIL import Image
import os
import sys

SUPPORT_EXTS = (".jpg", ".jpeg", ".webp", ".bmp", ".tiff")

def convert_single_image(file_path):
    if not os.path.exists(file_path):
        print(f"错误：文件不存在 {file_path}")
        return
    file_dir, full_name = os.path.split(file_path)
    name, ext = os.path.splitext(full_name)
    ext = ext.lower()
    if ext not in SUPPORT_EXTS:
        print(f"不支持的格式：{full_name}")
        return
    out_path = os.path.join(file_dir, f"{name}.png")
    try:
        with Image.open(file_path) as img:
            img.save(out_path, "PNG")
        print(f"转换完成：{full_name} → {name}.png")
    except Exception as e:
        print(f"转换失败 {full_name}：{str(e)}")

def convert_all_in_dir(folder):
    for filename in os.listdir(folder):
        full_path = os.path.join(folder, filename)
        if os.path.isfile(full_path):
            convert_single_image(full_path)

if __name__ == "__main__":
    work_dir = os.getcwd()
    # 命令行传入图片路径/名称
    if len(sys.argv) > 1:
        target_file = sys.argv[1]
        # 如果只传文件名，拼接当前目录
        if not os.path.isabs(target_file):
            target_file = os.path.join(work_dir, target_file)
        convert_single_image(target_file)
    else:
        print("未传入文件名，转换当前目录全部图片")
        #convert_all_in_dir(work_dir)
