package aho

import com.fasterxml.jackson.annotation.JsonProperty

data class ResponseFilm(
    @JsonProperty("items") val items: List<FilmData>
)

data class FilmData(
    @JsonProperty("name") val name: String?,
    @JsonProperty("slug") val slug: String?,
    @JsonProperty("poster_url") val posterUrl: String?,
    @JsonProperty("total_episodes") val totalEpisodes: Int?,
    @JsonProperty("current_episode") val currentEpisode: String?,
    @JsonProperty("quality") val quality: String?
)

data class ResponseMovie(
    @JsonProperty("movie") val movie: MovieData
)

data class MovieData(
    @JsonProperty("name") val name: String?,
    @JsonProperty("thumb_url") val thumbUrl: String?,
    @JsonProperty("description") val description: String?,
    @JsonProperty("total_episodes") val totalEpisodes: Int?,
    @JsonProperty("current_episode") val currentEpisode: String?,
    @JsonProperty("time") val time: String?,
    @JsonProperty("category") val category: Map<String, Category>,
    @JsonProperty("episodes") val episode: List<EpisodeData>
)

data class EpisodeData(
    @JsonProperty("server_name") val serverName: String?,
    @JsonProperty("items") val items: List<ServerData>
)

data class ServerData(
    @JsonProperty("name") val name: String?,
    @JsonProperty("slug") val slug: String?,
    @JsonProperty("embed") val embed: String?,
)

data class Category(
    @JsonProperty("group") val group: Group,
    @JsonProperty("list") val list: List<CategoryItem>
)

data class Group(
    @JsonProperty("name") val name: String?
)

data class CategoryItem(
    @JsonProperty("name") val name: String?
)