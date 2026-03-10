package com.inventory.config;

import com.inventory.factory.fixture.ItemFixture;
import com.inventory.factory.fixture.ItemListFixture;
import com.inventory.factory.fixture.UserFixture;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ItemListRepository itemListRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    public void run(String... args) {
        User admin = userRepository.findByEmail("admin@example.com").orElse(null);

        if (admin == null) {
            admin = userRepository.save(Objects.requireNonNull(UserFixture.createAdmin(passwordEncoder)));
            log.info("Created default admin user: admin@example.com / admin123");
        }

        if (isDevProfile() && itemListRepository.findByUserId(Objects.requireNonNull(admin.getId(), "Admin not found"), Pageable.unpaged()).isEmpty()) {
            List<ItemList> lists = ItemListFixture.createAll(admin);
            for (ItemList list : lists) {
                ItemFixture.createItemsFor(list);
            }
            itemListRepository.saveAll(Objects.requireNonNull(lists, "Lists not found"));
            int totalItems = lists.stream().mapToInt(l -> l.getItems().size()).sum();
            log.info("Seeded {} lists with {} items for admin user", lists.size(), totalItems);
        }
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}
