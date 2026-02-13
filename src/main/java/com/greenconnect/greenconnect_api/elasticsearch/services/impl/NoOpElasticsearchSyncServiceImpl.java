// import java.util.UUID;

// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.stereotype.Service;

// import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;

// import lombok.extern.slf4j.Slf4j;

// @Service
// @Slf4j
// @ConditionalOnProperty(value = "spring.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
// public class NoOpElasticsearchSyncServiceImpl implements ElasticsearchSyncService {

//     @Override
//     public void syncProduct(UUID productId) {
//         log.debug("🔄 [NO-OP] Elasticsearch disabled - skipping product sync: {}", productId);
//     }

//     @Override
//     public void syncAllProducts() {
//         log.debug("🔄 [NO-OP] Elasticsearch disabled - skipping sync all products");
//     }

//     @Override
//     public void syncCategory(UUID categoryId) {
//         log.debug("🔄 [NO-OP] Elasticsearch disabled - skipping category sync: {}", categoryId);
//     }

//     @Override
//     public void syncSupplier(UUID supplierId) {
//         log.debug("🔄 [NO-OP] Elasticsearch disabled - skipping supplier sync: {}", supplierId);
//     }

//     @Override
//     public void syncProductVariant(UUID productId) {
//         log.debug("🔄 [NO-OP] Elasticsearch disabled - skipping variant sync: {}", productId);
//     }

//     @Override
//     public void deleteProduct(UUID productId) {
//         log.debug("🔄 [NO-OP] Elasticsearch disabled - skipping product delete: {}", productId);
//     }

//     @Override
//     public boolean isElasticsearchHealthy() {
//         return false;
//     }
// }