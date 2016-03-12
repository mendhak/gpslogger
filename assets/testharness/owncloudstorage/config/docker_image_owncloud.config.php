<?php
// Custom options for this Docker image.
// https://github.com/jchaney/owncloud
$CONFIG = array (

    // Memory caching backend configuration: APC user backend

    'memcache.local' => '\OC\Memcache\APCu',

    // Install additional applications on persistent storage.
    'apps_paths' => array (
        0 => array (
            'path'     => OC::$SERVERROOT.'/apps',
            'url'      => '/apps',
            'writable' => false,
        ),
        1 => array (
            'path'     => OC::$SERVERROOT.'/apps_persistent',
            'url'      => '/apps_persistent',
            'writable' => true,
        ),
    ),

    // Don’t allow ownCloud update via the web interface.
    // https://github.com/jchaney/owncloud/issues/37
    'updatechecker' => false,
);
