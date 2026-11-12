package mx.loperstack.tokkiphealth.appcleaner.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Chat
import androidx.compose.material.icons.automirrored.twotone.FormatListBulleted
import androidx.compose.material.icons.automirrored.twotone.HelpOutline
import androidx.compose.material.icons.twotone.AdsClick
import androidx.compose.material.icons.twotone.Analytics
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.PermMedia
import androidx.compose.material.icons.twotone.PhotoLibrary
import androidx.compose.material.icons.twotone.SignalCellularOff
import androidx.compose.material.icons.twotone.SportsEsports
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.ExpendablesFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.AdvertisementFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.AnalyticsFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.BugReportingFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.CodeCacheFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.DefaultCachesPrivateFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.DefaultCachesPublicFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.GameFilesFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.HiddenFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.MobileQQFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.OfflineCacheFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.RecycleBinsFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.ShortcutServiceFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.TelegramFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.ThreemaFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.ThumbnailsFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.ViberFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.WeChatFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.WebViewCacheFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.WhatsAppBackupsFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.WhatsAppReceivedFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.WhatsAppSentFilter
import mx.loperstack.tokkiphealth.common.compose.icons.Bug
import mx.loperstack.tokkiphealth.common.compose.icons.Chrome
import mx.loperstack.tokkiphealth.common.compose.icons.Qq
import mx.loperstack.tokkiphealth.common.compose.icons.SdmIcons
import mx.loperstack.tokkiphealth.common.compose.icons.WeChat
import mx.loperstack.tokkiphealth.common.compose.icons.WhatsApp
import kotlin.reflect.KClass

@get:StringRes
val <T : ExpendablesFilter> KClass<T>.labelRes: Int
    get() = when (this) {
        DefaultCachesPublicFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_defaultcachespublic_label
        DefaultCachesPrivateFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_defaultcachesprivate_label
        HiddenFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_hiddencaches_label
        ThumbnailsFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_thumbnails_label
        CodeCacheFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_codecache_label
        AdvertisementFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_advertisement_label
        BugReportingFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_bugreporting_label
        AnalyticsFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_analytics_label
        GameFilesFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_gamefiles_label
        OfflineCacheFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_offlinecache_label
        RecycleBinsFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_recyclebins_label
        WebViewCacheFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_webview_label
        ShortcutServiceFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_shortcutservice_label
        WhatsAppBackupsFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_whatsapp_backups_label
        WhatsAppReceivedFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_whatsapp_received_label
        WhatsAppSentFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_whatsapp_sent_label
        TelegramFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_telegram_label
        ThreemaFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_threema_label
        WeChatFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_wechat_label
        ViberFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_viber_label
        MobileQQFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_qqchat_label
        else -> mx.loperstack.tokkiphealth.common.R.string.general_todo_msg
    }

@get:StringRes
val <T : ExpendablesFilter> KClass<T>.descriptionRes: Int
    get() = when (this) {
        DefaultCachesPublicFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_defaultcachespublic_summary
        DefaultCachesPrivateFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_defaultcachesprivate_summary
        HiddenFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_hiddencaches_summary
        ThumbnailsFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_thumbnails_summary
        CodeCacheFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_codecache_summary
        AdvertisementFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_advertisement_summary
        BugReportingFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_bugreporting_summary
        AnalyticsFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_analytics_summary
        GameFilesFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_gamefiles_summary
        OfflineCacheFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_offlinecache_summary
        RecycleBinsFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_recyclebins_summary
        WebViewCacheFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_webview_summary
        ShortcutServiceFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_shortcutservice_summary
        WhatsAppBackupsFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_whatsapp_backups_summary
        WhatsAppReceivedFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_whatsapp_received_summary
        WhatsAppSentFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_whatsapp_sent_summary
        TelegramFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_telegram_summary
        ThreemaFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_threema_summary
        WeChatFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_wechat_summary
        ViberFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_viber_summary
        MobileQQFilter::class -> mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_filter_qqchat_summary
        else -> mx.loperstack.tokkiphealth.common.R.string.general_todo_msg
    }

val <T : ExpendablesFilter> KClass<T>.icon: ImageVector
    get() = when (this) {
        DefaultCachesPublicFilter::class -> Icons.AutoMirrored.TwoTone.FormatListBulleted
        DefaultCachesPrivateFilter::class -> Icons.AutoMirrored.TwoTone.FormatListBulleted
        HiddenFilter::class -> Icons.TwoTone.VisibilityOff
        ThumbnailsFilter::class -> Icons.TwoTone.PermMedia
        CodeCacheFilter::class -> Icons.AutoMirrored.TwoTone.FormatListBulleted
        AdvertisementFilter::class -> Icons.TwoTone.AdsClick
        BugReportingFilter::class -> SdmIcons.Bug
        AnalyticsFilter::class -> Icons.TwoTone.Analytics
        GameFilesFilter::class -> Icons.TwoTone.SportsEsports
        OfflineCacheFilter::class -> Icons.TwoTone.SignalCellularOff
        RecycleBinsFilter::class -> Icons.TwoTone.Delete
        WebViewCacheFilter::class -> SdmIcons.Chrome
        ShortcutServiceFilter::class -> Icons.TwoTone.PhotoLibrary
        WhatsAppBackupsFilter::class -> SdmIcons.WhatsApp
        WhatsAppReceivedFilter::class -> SdmIcons.WhatsApp
        WhatsAppSentFilter::class -> SdmIcons.WhatsApp
        TelegramFilter::class -> Icons.AutoMirrored.TwoTone.Chat
        ThreemaFilter::class -> Icons.AutoMirrored.TwoTone.Chat
        WeChatFilter::class -> SdmIcons.WeChat
        ViberFilter::class -> Icons.AutoMirrored.TwoTone.Chat
        MobileQQFilter::class -> SdmIcons.Qq
        else -> Icons.AutoMirrored.TwoTone.HelpOutline
    }
