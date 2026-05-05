#!/bin/bash

# Path resolution
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Assuming script is in kone/scripts, so root is one level up
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
APK_DIR="$PROJECT_ROOT/app/build/intermediates/apk/debug"

BUCKET="kone-apk"
OSS_PREFIX="apk/"
REGION="oss-cn-shanghai"

# Find the newest apk file
LATEST_APK=$(ls -t "$APK_DIR"/*.apk 2>/dev/null | head -n 1)

if [ -z "$LATEST_APK" ]; then
    echo "Error: No APK found in $APK_DIR"
    exit 1
fi

FILE_NAME=$(basename "$LATEST_APK")

echo "Found latest APK: $FILE_NAME"
echo "Uploading to oss://$BUCKET/$OSS_PREFIX$FILE_NAME ..."

# Using aliyun CLI
# Note: This assumes aliyun CLI is already configured with credentials.
aliyun oss cp "$LATEST_APK" "oss://$BUCKET/$OSS_PREFIX$FILE_NAME" --force

if [ $? -eq 0 ]; then
    echo "--------------------------------------------------"
    echo "Upload successful!"
    echo "Public URL: https://$BUCKET.$REGION.aliyuncs.com/$OSS_PREFIX$FILE_NAME"
    echo "--------------------------------------------------"
else
    echo "Error: Upload failed!"
    exit 1
fi
