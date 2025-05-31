// use an integer for version numbers
version = 1

cloudstream {
    language = "vi"
    // All of these properties are optional, you can safely remove them
    description = "Movies (NguonC)" // tên Nguồn Phim
    authors = listOf("Aho")
    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Movies",
    )

    iconUrl = "https://phim.nguonc.com/public/images/Logo/logonc.png" // thay đổi url ảnh khi tạo Plugin mới

    isCrossPlatform = false

}

dependencies {
    // FIXME remove this when crossplatform is fully supported
    val cloudstream by configurations
    cloudstream("com.lagradost:cloudstream3:pre-release")
}