package com.mendhak.gpslogger.senders;

import java.io.File;
import java.util.List;

public interface IFileSender
{
    /// Upload or send this specific file
    void UploadFile(List<File> files);


}
