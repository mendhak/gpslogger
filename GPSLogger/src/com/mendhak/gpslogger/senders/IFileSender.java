package com.mendhak.gpslogger.senders;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public interface IFileSender extends FilenameFilter
{
    /// Upload or send this specific file
    void UploadFile(List<File> files);


}
