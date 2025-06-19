package aho

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup

class SDFIMProvider : MainAPI() {
    override var name = "SDFIM"
    override var mainUrl = "https://sdfim.org"
    override var lang = "vi"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = mainUrl
        val document = app.get(url).document
        val homePageList = ArrayList<HomePageList>()

        // Phim Chiếu Rạp
        val phimChieuRapTitle = "Phim Chiếu Rạp"
        val phimChieuRapElements = document.select("div.halim-list-item:has(a[title*=\'Phim Chiếu Rạp\']) > div.halim-item")
        if (phimChieuRapElements.isNotEmpty()) {
            val phimChieuRapList = phimChieuRapElements.mapNotNull { 
                val title = it.selectFirst("a")?.attr("title") ?: return@mapNotNull null
                val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val posterUrl = it.selectFirst("img")?.attr("data-src")
                newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl
                }
            }
            homePageList.add(HomePageList(phimChieuRapTitle, phimChieuRapList))
        }
        
        // Phim Lẻ
        val phimLeTitle = "Phim Lẻ"
        val phimLeElements = document.select("div.halim-list-item:has(a[title*=\'Phim Lẻ\']) > div.halim-item")
        if (phimLeElements.isNotEmpty()) {
            val phimLeList = phimLeElements.mapNotNull { 
                val title = it.selectFirst("a")?.attr("title") ?: return@mapNotNull null
                val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val posterUrl = it.selectFirst("img")?.attr("data-src")
                newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl
                }
            }
            homePageList.add(HomePageList(phimLeTitle, phimLeList))
        }

        // Phim Bộ
        val phimBoTitle = "Phim Bộ"
        val phimBoElements = document.select("div.halim-list-item:has(a[title*=\'Phim Bộ\']) > div.halim-item")
        if (phimBoElements.isNotEmpty()) {
            val phimBoList = phimBoElements.mapNotNull { 
                val title = it.selectFirst("a")?.attr("title") ?: return@mapNotNull null
                val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val posterUrl = it.selectFirst("img")?.attr("data-src")
                newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                    this.posterUrl = posterUrl
                }
            }
            homePageList.add(HomePageList(phimBoTitle, phimBoList))
        }

        return HomePageResponse(homePageList)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val response = app.get(url).text
        val document = Jsoup.parse(response)
        val movies = document.select("div.halim-item")
        val searchResponses = ArrayList<SearchResponse>()
        for (movie in movies) {
            val title = movie.selectFirst("a")?.attr("title") ?: continue
            val posterUrl = movie.selectFirst("img")?.attr("data-src")
            val movieUrl = movie.selectFirst("a")?.attr("href") ?: continue
            // Determine TvType based on URL or other indicators if possible
            val tvType = if (movieUrl.contains("/series/")) TvType.TvSeries else TvType.Movie
            searchResponses.add(newSearchResponse(title, movieUrl, tvType) {
                this.posterUrl = posterUrl
            })
        }
        return searchResponses
    }

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(url).text
        val document = Jsoup.parse(response)

        val title = document.selectFirst("h1.entry-title")?.text() ?: document.selectFirst("meta[property=og:title]")?.attr("content") ?: return null
        val posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content")
        val plot = document.selectFirst("div.entry-content > p")?.text()
        val year = document.selectFirst("span.year")?.text()?.toIntOrNull()
        val tags = document.select("div.film-info-genre a[rel=tag]").map { it.text() }

        if (url.contains("/series/")) {
            val episodes = ArrayList<Episode>()
            document.select("div.episodes-list-content a").forEach {
                val episodeUrl = it.attr("href")
                val episodeName = it.text()
                episodes.add(Episode(episodeUrl, episodeName))
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.year = year
                this.tags = tags
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.year = year
                this.tags = tags
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val response = app.get(data).text
        val document = Jsoup.parse(response)
        val iframeSrc = document.selectFirst("iframe[src*=drive.google.com]")?.attr("src")
        if (iframeSrc != null) {
            loadExtractor(iframeSrc, mainUrl, subtitleCallback, callback)
            return true
        }
        return false
    }
}

