package io.crnk.jpa.internal;

import io.crnk.core.engine.internal.utils.PreconditionUtil;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.IncludeSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.queryspec.SortSpec;
import io.crnk.jpa.JpaModuleConfig;
import io.crnk.jpa.JpaRepositoryConfig;
import io.crnk.jpa.query.JpaQuery;
import io.crnk.jpa.query.JpaQueryExecutor;
import io.crnk.meta.model.MetaAttribute;
import io.crnk.meta.model.MetaDataObject;
import io.crnk.meta.model.MetaKey;

import java.util.Arrays;
import java.util.Set;

public class JpaRepositoryUtils {

	private JpaRepositoryUtils() {
	}

	/**
	 * @param meta of the entity
	 * @return Gets the primary key attribute of the given entity. Assumes a
	 * primary key is available and no compound primary keys are
	 * supported.
	 */
	public static MetaAttribute getPrimaryKeyAttr(MetaDataObject meta) {
		MetaKey primaryKey = meta.getPrimaryKey();
		PreconditionUtil.verify(primaryKey != null, "no primary key for %s", meta);
		PreconditionUtil.verifyEquals(1, primaryKey.getElements().size(), "non-compound primary key expected for %s", meta);
		return primaryKey.getElements().get(0);
	}

	public static void prepareQuery(JpaQuery<?> query, QuerySpec querySpec, Set<String> computedAttrs) {

		for (String computedAttr : computedAttrs) {
			query.addSelection(Arrays.asList(computedAttr));
		}

		for (FilterSpec filter : querySpec.getFilters()) {
			query.addFilter(filter);
		}
		for (SortSpec sortSpec : querySpec.getSort()) {
			query.addSortBy(sortSpec);
		}
		if (!querySpec.getIncludedFields().isEmpty()) {
			throw new UnsupportedOperationException("includeFields not yet supported");
		}

	}

	public static void prepareExecutor(JpaQueryExecutor<?> executor, QuerySpec querySpec, boolean includeRelations) {
		if (includeRelations) {
			for (IncludeSpec included : querySpec.getIncludedRelations()) {
				executor.fetch(included.getAttributePath());
			}
		}
		executor.setOffset((int) querySpec.getOffset());
		if (querySpec.getOffset() > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("offset cannot be larger than Integer.MAX_VALUE");
		}
		if (querySpec.getLimit() != null) {
			if (querySpec.getLimit() > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("limit cannot be larger than Integer.MAX_VALUE");
			}
			executor.setLimit((int) querySpec.getLimit().longValue());
		}
	}

	public static void setDefaultConfig(JpaModuleConfig moduleConfig, JpaRepositoryConfig<?> repositoryConfig) {
		if (!repositoryConfig.hasQueryFactory()) {
			repositoryConfig.setQueryFactory(moduleConfig::getQueryFactory);
		}
		if (!repositoryConfig.hasTotalAvailable()) {
			repositoryConfig.setTotalAvailable(moduleConfig.isTotalResourceCountUsed());
		}

		moduleConfig.getFilters().stream()
				.filter(filter -> !repositoryConfig.getFilters().contains(filter))
				.forEach(filter -> repositoryConfig.addFilter(filter));
	}
}
