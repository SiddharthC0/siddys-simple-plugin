
package org.siddharth.ssmp.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.siddharth.ssmp.economy.AuctionHouse;

import java.util.ArrayList;
import java.util.List;

public class AuctionHouseGUI implements Listener {

    private final AuctionHouse auctionHouse;

    public AuctionHouseGUI(AuctionHouse auctionHouse) {
        this.auctionHouse = auctionHouse;
    }

    public void openSellDialog(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            auctionHouse.error(player, "Your main hand is empty.");
            return;
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Create Auction Listing"))
                        .body(List.of(
                                DialogBody.item(hand.clone()).build(),
                                DialogBody.plainMessage(Component.text(
                                        "Enter your listing price."
                                ))
                        ))
                        .inputs(List.of(
                                io.papermc.paper.registry.data.dialog.input.DialogInput
                                        .text("price", Component.text("Price"))
                                        .maxLength(10)
                                        .initial("")
                                        .build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Continue"))
                                .action(DialogAction.customClick((view, audience) -> {
                                    if (!(audience instanceof Player seller)) return;

                                    String input = view.getText("price");

                                    if (input == null || input.isBlank()) {
                                        auctionHouse.error(seller, "Enter a listing price.");
                                        return;
                                    }

                                    int price;

                                    try {
                                        price = Integer.parseInt(input.trim());
                                    } catch (NumberFormatException e) {
                                        auctionHouse.error(seller, "Enter a valid whole-number price.");
                                        return;
                                    }

                                    if (price <= 0) {
                                        auctionHouse.error(seller, "The minimum listing price is $1.");
                                        return;
                                    }

                                    if (price > auctionHouse.getMaxAuctionPrice()) {
                                        auctionHouse.error(
                                                seller,
                                                "The maximum auction price is $" +
                                                        auctionHouse.getMaxAuctionPrice()
                                        );
                                        return;
                                    }

                                    openListingConfirmation(seller, price);
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Cancel")).build()
                ))
        );

        player.showDialog(dialog);
    }

    private void openListingConfirmation(Player player, int price) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            auctionHouse.error(player, "Your main hand is empty.");
            return;
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Confirm Listing"))
                        .body(List.of(
                                DialogBody.item(item.clone()).build(),
                                DialogBody.plainMessage(Component.text(
                                        "List " + auctionHouse.getItemName(item) +
                                                " for $" + price + "?"
                                ))
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("List for $" + price))
                                .action(DialogAction.customClick((view, audience) -> {
                                    if (audience instanceof Player seller) {
                                        auctionHouse.createListing(seller, price);
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Cancel")).build()
                ))
        );

        player.showDialog(dialog);
    }

    public void openAuctionHouse(Player player, int page) {
        int maxPage = auctionHouse.getMaxPage();
        page = Math.max(1, Math.min(page, maxPage));

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                ChatColor.DARK_GRAY + "Auction House | " + page
        );

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, glass);
            inventory.setItem(45 + slot, glass);
        }

        List<Integer> ids = auctionHouse.getListingIds();

        int start = (page - 1) * auctionHouse.getListingsPerPage();
        int end = Math.min(start + auctionHouse.getListingsPerPage(), ids.size());
        int guiSlot = 9;

        for (int i = start; i < end; i++) {
            int id = ids.get(i);
            ItemStack display = createListingDisplay(id);

            if (display != null) {
                setListingId(display, id);
                inventory.setItem(guiSlot, display);
                guiSlot++;
            }
        }

        if (ids.isEmpty()) {
            ItemStack empty = createItem(Material.BARRIER, ChatColor.RED + "No auctions today");
            ItemMeta meta = empty.getItemMeta();

            if (meta != null) {
                meta.setLore(List.of(ChatColor.GRAY + "There are currently no items listed."));
                empty.setItemMeta(meta);
            }

            inventory.setItem(22, empty);
        }

        ItemStack head = createPlayerHead(player);

        if (head != null) {
            inventory.setItem(45, head);
        }

        if (page > 1) {
            inventory.setItem(48, createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        }

        inventory.setItem(
                49,
                createItem(Material.PAPER, ChatColor.YELLOW + "Page " + page + "/" + maxPage)
        );

        if (page < maxPage) {
            inventory.setItem(50, createItem(Material.ARROW, ChatColor.YELLOW + "Next Page"));
        }

        player.openInventory(inventory);
    }

    private void openPurchaseConfirmation(Player player, int id) {
        if (!auctionHouse.listingExists(id)) {
            auctionHouse.error(player, "That auction is no longer available.");
            return;
        }

        ItemStack item = auctionHouse.getListingItem(id);

        if (item == null || item.getType().isAir()) {
            auctionHouse.error(player, "That auction is invalid.");
            return;
        }

        int price = auctionHouse.getListingPrice(id);
        String seller = auctionHouse.getListingSellerName(id);
        String itemName = auctionHouse.getItemName(item);

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Purchase Auction"))
                        .body(List.of(
                                DialogBody.item(item.clone()).build(),
                                DialogBody.plainMessage(Component.text(
                                        itemName + "  |  $" + price
                                )),
                                DialogBody.plainMessage(Component.text(
                                        "Seller: " + seller
                                ))
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Buy for $" + price))
                                .action(DialogAction.customClick((view, audience) -> {
                                    if (!(audience instanceof Player buyer)) return;

                                    if (!auctionHouse.getPermissions().hasPermission(
                                            buyer, "ssmp.auction.buy")) {
                                        auctionHouse.error(
                                                buyer,
                                                "You do not have permission to buy auctions."
                                        );
                                        return;
                                    }

                                    auctionHouse.buyListing(buyer, id);
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build(),
                        ActionButton.builder(Component.text("Cancel")).build()
                ))
        );

        auctionHouse.getSounds().play(player, "menu-click");
        player.showDialog(dialog);
    }

    private ItemStack createListingDisplay(int id) {
        ItemStack item = auctionHouse.getListingItem(id);

        if (item == null) return null;

        ItemStack display = item.clone();
        ItemMeta meta = display.getItemMeta();

        if (meta == null) return display;

        List<String> lore = meta.hasLore()
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        lore.add("");
        lore.add(ChatColor.GRAY + "Seller: " + auctionHouse.getListingSellerName(id));
        lore.add(ChatColor.GOLD + "Price: $" + auctionHouse.getListingPrice(id));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view");

        meta.setLore(lore);
        display.setItemMeta(meta);

        return display;
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta == null) return head;

        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.YELLOW + player.getName());
        meta.setLore(List.of(
                "",
                ChatColor.GRAY + "Balance: " + ChatColor.GREEN + "$" +
                        auctionHouse.getEconomy().getPlayerBalance(player)
        ));

        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void setListingId(ItemStack item, int id) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    auctionHouse.getListingKey(),
                    PersistentDataType.INTEGER,
                    id
            );
            item.setItemMeta(meta);
        }
    }

    private int getListingId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;

        Integer id = item.getItemMeta()
                .getPersistentDataContainer()
                .get(auctionHouse.getListingKey(), PersistentDataType.INTEGER);

        return id == null ? -1 : id;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!title.startsWith("Auction House | ")) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) ||
                event.getRawSlot() < 0 ||
                event.getRawSlot() >= top.getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null) return;

        int slot = event.getRawSlot();

        if (slot == 48) {
            int page = getCurrentPage(title);

            if (page > 1) {
                auctionHouse.getSounds().play(player, "menu-click");
                openAuctionHouse(player, page - 1);
            }

            return;
        }

        if (slot == 50) {
            int page = getCurrentPage(title);

            if (page < auctionHouse.getMaxPage()) {
                auctionHouse.getSounds().play(player, "menu-click");
                openAuctionHouse(player, page + 1);
            }

            return;
        }

        int id = getListingId(clicked);

        if (id != -1 && auctionHouse.listingExists(id)) {
            if (!auctionHouse.getPermissions().hasPermission(player, "ssmp.auction.buy")) {
                auctionHouse.error(player, "You do not have permission to buy auctions.");
                return;
            }
            openPurchaseConfirmation(player, id);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (title.startsWith("Auction House | ")) {
            event.setCancelled(true);
        }
    }

    private int getCurrentPage(String title) {
        try {
            return Integer.parseInt(title.substring(title.lastIndexOf("| ") + 2));
        } catch (Exception e) {
            return 1;
        }
    }
}
