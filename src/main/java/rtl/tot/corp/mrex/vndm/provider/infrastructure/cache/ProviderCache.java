package rtl.tot.corp.mrex.vndm.provider.infrastructure.cache;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import rtl.tot.corp.mrex.vndm.provider.domain.entity.Provider;
import rtl.tot.corp.mrex.vndm.provider.domain.util.Util;
import rtl.tot.corp.mrex.vndm.provider.infrastructure.configuration.RedisCacheConfiguration;

@Service
@Slf4j
public class ProviderCache {
    
    @Autowired
    private RedisCacheConfiguration cacheConfiguration;

    private final ObjectMapper mapper;

    public ProviderCache() {
        this.mapper = new ObjectMapper();
    }

    private Jedis createClient() {
        JedisShardInfo shardInfo = new JedisShardInfo(cacheConfiguration.getHost(),cacheConfiguration.getPort(), true);
        shardInfo.setPassword(this.cacheConfiguration.getKey());
        return new Jedis(shardInfo);
    }

    public Optional<Provider>  getProviders(Provider provider) {
        log.info("Attempting to retrieve the providers from cache");
        Jedis client = createClient();
        String resultString = client.get(Util.generateKey(provider.getRut(), provider.getCountryCode()));
        client.close();
        log.info("Cache Data Returned: " + resultString);
        if(Objects.nonNull(resultString )) {
          try {
              Optional<Provider> prov =Optional.of(mapper.readValue(resultString, Provider.class));
              return prov;
          }
          catch (IOException e) {
              log.error("Cache content is of unknown format: {}", resultString);
          }
          catch (Exception e) {
            log.error("General Exception: {}", e.getMessage());
          }
        }
        return null;
    }
       
    void addProviderCache(Provider provider) {
      log.info("Add provider to Cache");
      try {
          String jsonAsString = mapper.writeValueAsString(provider);
          log.info("Cache Data Saved : " + jsonAsString);
          Jedis client = createClient(); 
          client.set(Util.generateKey(provider.getRut(), provider.getCountryCode()), jsonAsString);
          client.close();
      }
      catch (IOException ex) {
          log.error("Could not cache the provider: ", ex.getMessage(), ex);
      }
  }

}
