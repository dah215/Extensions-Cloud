package aho

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.NiceResponse

class NguonC : MainAPI() {
    override var mainUrl = "https://phim.nguonc.com"
    private var directUrl = "$mainUrl/api/films"
    override var name = "NguonC"
    override val hasMainPage = true
    override var lang = "vi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/api/films/phim-moi-cap-nhat?page=" to "Mới Cập Nhật",
        "$mainUrl/api/films/danh-sach/phim-le?page=" to "Phim Lẻ",
        "$mainUrl/api/films/danh-sach/phim-bo?page=" to "Phim Bộ",
    )

    // Cloudflare bypass headers
    override fun getGlobalRequestInterceptor(): Interceptor? {
        return Interceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-User", "?1")
                .header("Referer", "$mainUrl/")
                .build()
            chain.proceed(request)
        }
    }

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse? {
        val response = app.get("${request.data}$page", timeout = 60).parsed<ResponseFilm>()
        val home = response.items.mapNotNull { toSearchResult(it) }
        return newHomePageResponse(
            HomePageList(request.name, home),
            hasNext = response.items.isNotEmpty()
        )
    }

    private fun toSearchResult(filmData: FilmData): SearchResponse? {
        val href = "$mainUrl/api/film/${filmData.slug}"
        val poster = filmData.posterUrl
        val title = filmData.name.toString()
        val type = filmData.currentEpisode
        val quality = filmData.quality.toString()

        return if (filmData.totalEpisodes == null || filmData.totalEpisodes != 1) {
            val episode = type?.substringAfter("Tập ")?.substringBefore("/")?.toIntOrNull()
            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = poster
                addSub(episode)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
                addQuality(quality)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val request = app.get(
            url = "$directUrl/search?keyword=$query",
            timeout = 60,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        ).parsed<ResponseFilm>()
        
        return request.items.mapNotNull { toSearchResult(it) }
    }

    override suspend fun load(url: String): LoadResponse? {
        val request = app.get(url, timeout = 60).parsed<ResponseMovie>()
        val movie = request.movie

        val title = movie.name.toString()
        val poster = movie.thumbUrl
        val description = movie.description?.substringAfter("<p>")?.substringBefore("</p>")
        val year = movie.category["3"]?.list?.getOrNull(0)?.name?.toIntOrNull()
        val tags = movie.category["2"]?.list?.mapNotNull { it.name.toString() }

        return if (movie.totalEpisodes == 1) {
            val link = movie.episode.getOrNull(0)?.items?.getOrNull(0)?.embed
            newMovieLoadResponse(title, url, TvType.Movie, link) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
            }
        } else {
            val epSub = movie.episode.getOrNull(0)?.items?.mapNotNull {
                it.embed?.let { embed ->
                    Episode(
                        data = embed,
                        name = "Episode ${it.name}",
                        episode = it.name?.toIntOrNull()
                    )
                }
            } ?: emptyList()

            val episodeMap = mutableMapOf(DubStatus.Subbed to epSub)

            if (movie.episode.size == 2) {
                val epDub = movie.episode.getOrNull(1)?.items?.mapNotNull {
                    it.embed?.let { embed ->
                        Episode(
                            data = embed,
                            name = "Episode ${it.name}",
                            episode = it.name?.toIntOrNull()
                        )
                    }
                } ?: emptyList()
                episodeMap[DubStatus.Dubbed] = epDub
            }

            newAnimeLoadResponse(title, url, TvType.TvSeries) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.episodes = episodeMap
            }
        }
    }

    // Video interceptor for playback
    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor? {
        return Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .removeHeader("Host")
                .header("Referer", "$mainUrl/")
                .header("Origin", mainUrl)
                .header("Sec-Fetch-Dest", "video")
                .header("Sec-Fetch-Mode", "no-cors")
                .header("Sec-Fetch-Site", "same-origin")
                .build()
            chain.proceed(newRequest)
        }
    }

    // Cloudflare bypass for video links
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Add Cloudflare bypass cookies if available
        val cookies = listOf(
            "J1GAOLnZtfov.7MId7Z7bF.7sHScBrxE_UdYspqiSg8-1748666256-1.2.1.1-48rIhWvsIvxBuXwU5qtVNXWiPNyUEBZLJfTW4l2QCassc0AiATYHiggSy_4Pk1j4u9oNuvZkJ7B5YCIVsuF9rhfvL4U.8HQUtrKsLC_ei9Xn0ocBPJZEMbmienmuR6ISf2wGcn2qXYAsgFd66tZ4QDWlYEYMFqVWZFpeh84dDxTzEJ95djiMMK62rMuG52vysOrBLlyl78alUvLXFhX5dmlAtj36DmEwAWYlaF2nX28v6fo1_SoRvow.QiNQ.hjrYpvzAsd78iYN6cg6XW4z5E69z5FahVw9z7dSXCt6BKRIxHi5zLzpLMFpauN7svrOcQ5yBbCsr.lm3eGs4DBJGBS2HNhXMWZT9wrhGig6x1.nG3oUjrvAtnYEfZ1GlnOp", // Thay thế bằng cookie thực
            "2WlVDFSZ_IX3Mj1423sIB4O3HcI8PNAfBz0BJjEbzz4-1748666392-1.0.1.1-6fzi3wtQWIOu9SltDKfMgNMATCffc5lfWYdyYhGJr9AcRjC2NkY3xVrgzHV3igiMWKM1vymvAJ_zOi1HRj9mOtBO3EU809O_Mp5.ey1WJhE"              // Thay thế bằng cookie thực
        ).joinToString("; ")

        // Extract actual video URL
        val videoUrl = if (data.contains("embed.php")) {
            data.replace("embed.php", "get.php")
        } else {
            data
        }

        callback.invoke(
            ExtractorLink(
                source = name,
                name = name,
                url = videoUrl,
                referer = "$mainUrl/",
                quality = Qualities.P1080.value,
                headers = mapOf(
                    "Cookie" to cookies,
                    "Accept" to "*/*",
                    "Sec-Fetch-Dest" to "video",
                    "Sec-Fetch-Mode" to "no-cors"
                ),
                type = ExtractorLinkType.M3U8
            )
        )
        return true
    }
}