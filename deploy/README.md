# Original interface with family photos

Use config.json with the original tiles and controls plus the large clock/greeting header.
Family photos fill/crop the screen. A privately provisioned photos.json with shared_album_url is stored
inside the app by the permission-protected provider; never add the actual URL or images to this directory.
The existing local wallpaper is a fallback while the first complete album is cached.

Settings opens Android TV settings directly; Wi-Fi and Inputs remain visible. The dock stays Netflix,
Prime Video, Disney+, YouTube. Original library, reorder, tile-menu and module-card behavior remains.
No custom app-grid layout is added. Credentials, images and the persisted shuffle ledger stay outside Git.
