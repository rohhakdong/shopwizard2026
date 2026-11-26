package com.shopwizard.operate.model;

import java.util.List;

/** 목록 응답: 전체 건수와 이 쪽의 행. */
public record ListPage<T>(int total, List<T> rows) {}
