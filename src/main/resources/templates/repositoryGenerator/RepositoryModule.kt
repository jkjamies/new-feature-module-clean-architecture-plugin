package ${PACKAGE}.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ${DOMAIN_PACKAGE}.repository.${REPOSITORY_NAME}
import ${DATA_PACKAGE}.repository.${REPOSITORY_IMPL_NAME}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bind${REPOSITORY_NAME}(impl: ${REPOSITORY_IMPL_NAME}): ${REPOSITORY_NAME}
}
