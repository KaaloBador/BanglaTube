package com.redowan

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element
import java.util.ArrayList

class BanglaTubeProvider : MainAPI() {
    override var mainUrl = "http://banglatube.net"
    override var name = "BanglaTube"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "bn"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageResponse? {
        val document = app.get(mainUrl).document
        val homePages = ArrayList<HomePageList>()
        
        val items = document.select("div.video-item, div.post-box, article").mapNotNull {
            it.toSearchResult()
        }
        
        if (items.isNotEmpty()) {
            homePages.add(HomePageList("Recent Uploads", items))
        }
        return HomePageResponse(homePages)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3, .title, h2")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val completeHref = if (href.startsWith("http")) href else mainUrl + href
        val posterUrl = this.selectFirst("img")?.attr("src")

        return MovieSearchResponse(title, completeHref, this@BanglaTubeProvider.name, TvType.Movie, posterUrl)
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1, .video-title")?.text() ?: "BanglaTube Video"
        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")

        return MovieLoadResponse(
            title, url, this.name, TvType.Movie, url, poster, null, null
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        val videoSrc = document.selectFirst("iframe")?.attr("src") 
            ?: document.selectFirst("video source")?.attr("src")
            ?: document.selectFirst("video")?.attr("src")

        if (videoSrc != null) {
            val completeVideoUrl = if (videoSrc.startsWith("http")) videoSrc else mainUrl + videoSrc
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    completeVideoUrl,
                    referer = mainUrl,
                    quality = Qualities.Unknown.value
                )
            )
            return true
        }
        return false
    }
}
