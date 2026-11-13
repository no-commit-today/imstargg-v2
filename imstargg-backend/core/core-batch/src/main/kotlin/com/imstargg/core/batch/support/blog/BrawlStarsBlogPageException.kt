package com.imstargg.core.batch.support.blog

/** 공식 블로그 페이지가 기대한 모양과 달라 읽을 수 없을 때 던진다. */
class BrawlStarsBlogPageException(message: String) : RuntimeException(message)
