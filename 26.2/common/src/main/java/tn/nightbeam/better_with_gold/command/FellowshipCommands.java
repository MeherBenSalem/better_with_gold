package tn.nightbeam.better_with_gold.command;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.Fellowship;
import tn.nightbeam.better_with_gold.ally.Sellswords;
import tn.nightbeam.better_with_gold.ally.WarbandState;
import tn.nightbeam.better_with_gold.ally.Warbands;
import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.core.AllyClass;
import tn.nightbeam.better_with_gold.core.Chronicle;
import tn.nightbeam.better_with_gold.core.ClassCatalog;
import tn.nightbeam.better_with_gold.core.Formation;
import tn.nightbeam.better_with_gold.core.Order;
import tn.nightbeam.better_with_gold.core.Progression;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /fellowship: orders, formations, camp, chronicles, and admin tools for packs, servers and testing. */
public final class FellowshipCommands {
	private FellowshipCommands() {
	}

	private static final SuggestionProvider<CommandSourceStack> CLASSES = (ctx, builder) -> {
		List<String> ids = new ArrayList<>();
		for (AllyClass c : ClassCatalog.enabled()) {
			ids.add(c.id);
		}
		return SharedSuggestionProvider.suggest(ids, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> NAMES = (ctx, builder) -> {
		List<String> names = new ArrayList<>();
		names.add("all");
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player != null) {
			WarbandState s = Warbands.state(player.getUUID());
			s.seen.values().forEach(v -> names.add(v.name));
			s.stowed.forEach(st -> names.add(tn.nightbeam.better_with_gold.core.AllyRecord.fromJson(st.record).name));
		}
		return SharedSuggestionProvider.suggest(names, builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("fellowship")
				.executes(ctx -> summary(ctx.getSource()));
		LiteralArgumentBuilder<CommandSourceStack> order = Commands.literal("order");
		for (Order o : Order.values()) {
			order.then(Commands.literal(o.id()).executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				Warbands.order(p, o, o == Order.FOCUS ? Fellowship.lookedAt(p, 48) : null);
				return 1;
			}));
		}
		root.then(order);
		LiteralArgumentBuilder<CommandSourceStack> formation = Commands.literal("formation");
		for (Formation f : Formation.values()) {
			formation.then(Commands.literal(f.id()).executes(ctx -> {
				Warbands.formation(ctx.getSource().getPlayerOrException(), f);
				return 1;
			}));
		}
		root.then(formation);
		root.then(Commands.literal("roster").executes(ctx -> {
			Warbands.openRoster(ctx.getSource().getPlayerOrException(), null);
			return 1;
		}));
		root.then(Commands.literal("recall").executes(ctx -> {
			int n = Warbands.recall(ctx.getSource().getPlayerOrException());
			ctx.getSource().sendSuccess(() -> Component.literal("Recalling " + n + " " + (n == 1 ? "ally" : "allies") + "."), false);
			return n;
		}));
		root.then(Commands.literal("summon").executes(ctx -> summon(ctx, "all"))
				.then(Commands.argument("name", StringArgumentType.greedyString()).suggests(NAMES).executes(ctx -> summon(ctx, StringArgumentType.getString(ctx, "name")))));
		root.then(Commands.literal("stow")
				.then(Commands.argument("name", StringArgumentType.greedyString()).suggests(NAMES).executes(ctx -> {
					int n = Warbands.stowByName(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"));
					ctx.getSource().sendSuccess(() -> Component.literal(n + " sent to camp."), false);
					return n;
				})));
		root.then(Commands.literal("dismiss")
				.then(Commands.argument("name", StringArgumentType.greedyString()).suggests(NAMES).executes(ctx -> {
					boolean ok = Warbands.dismiss(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"));
					if (!ok) {
						ctx.getSource().sendFailure(Component.literal("No ally by that name."));
					}
					return ok ? 1 : 0;
				})));
		root.then(Commands.literal("chronicle")
				.then(Commands.argument("name", StringArgumentType.greedyString()).suggests(NAMES).executes(ctx -> chronicle(ctx, StringArgumentType.getString(ctx, "name")))));
		root.then(Commands.literal("remembrance").executes(FellowshipCommands::remembrance));
		root.then(Commands.literal("banner").then(Commands.literal("lift").executes(ctx -> Warbands.liftBanner(ctx.getSource().getPlayerOrException()) ? 1 : 0)));

		// Admin tools: packs, servers, creative and testing.
		root.then(Commands.literal("recruit").requires(s -> Compat.hasPermission(s, 2))
				.then(Commands.argument("class", StringArgumentType.word()).suggests(CLASSES)
						.executes(ctx -> recruit(ctx, 1))
						.then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(ctx -> recruit(ctx, IntegerArgumentType.getInteger(ctx, "level"))))));
		root.then(Commands.literal("sellsword").requires(s -> Compat.hasPermission(s, 2))
				.executes(ctx -> sellsword(ctx, null, 0))
				.then(Commands.argument("class", StringArgumentType.word()).suggests(CLASSES)
						.executes(ctx -> sellsword(ctx, StringArgumentType.getString(ctx, "class"), 0))
						.then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
								.executes(ctx -> sellsword(ctx, StringArgumentType.getString(ctx, "class"), IntegerArgumentType.getInteger(ctx, "level"))))));
		root.then(Commands.literal("setlevel").requires(s -> Compat.hasPermission(s, 2))
				.then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(ctx -> setLevel(ctx, IntegerArgumentType.getInteger(ctx, "level")))));
		root.then(Commands.literal("reload").requires(s -> Compat.hasPermission(s, 2)).executes(ctx -> {
			Fellowship.reload();
			int problems = ClassCatalog.problems().size();
			ctx.getSource().sendSuccess(() -> Component.literal("Fellowship config and " + ClassCatalog.all().size() + " classes reloaded"
					+ (problems == 0 ? "." : "; " + problems + " class file problem(s), see the log.")), true);
			return 1;
		}));
		dispatcher.register(root);
	}

	private static int summary(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer p = source.getPlayerOrException();
		source.sendSuccess(() -> Component.literal("Your warband: " + Warbands.describe(p.getUUID()) + ". Open the roster with /fellowship roster.")
				.withStyle(ChatFormatting.GOLD), false);
		return 1;
	}

	private static int summon(CommandContext<CommandSourceStack> ctx, String which) throws CommandSyntaxException {
		int n = Warbands.summon(ctx.getSource().getPlayerOrException(), which);
		ctx.getSource().sendSuccess(() -> Component.literal(n + " summoned from camp."), false);
		return n;
	}

	private static int chronicle(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		for (AllyEntity a : Warbands.liveAllies(p.getUUID())) {
			if (a.record().name.equalsIgnoreCase(name)) {
				ctx.getSource().sendSuccess(() -> Component.literal(a.record().fullName() + ", " + a.allyClass().name + " " + a.record().level
						+ " (" + Progression.title(a.record().level) + "): " + Chronicle.headline(a.record())).withStyle(ChatFormatting.GOLD), false);
				for (String line : Chronicle.lines(a.record().chronicle)) {
					ctx.getSource().sendSuccess(() -> Component.literal(" " + line).withStyle(ChatFormatting.GRAY), false);
				}
				return 1;
			}
		}
		ctx.getSource().sendFailure(Component.literal("No ally by that name nearby. The roster (/fellowship roster) shows everyone."));
		return 0;
	}

	private static int remembrance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		List<WarbandState.Fallen> fallen = Warbands.state(p.getUUID()).fallen;
		if (fallen.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("No one has fallen under your banner.").withStyle(ChatFormatting.GRAY), false);
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Remembrance").withStyle(ChatFormatting.GOLD), false);
		for (WarbandState.Fallen f : fallen) {
			ctx.getSource().sendSuccess(() -> Component.literal(" " + f.name + ", " + ClassCatalog.get(f.classId).name + " " + f.level
					+ ". Fell on day " + f.day + " to " + f.cause + ". " + f.headline + ".").withStyle(ChatFormatting.GRAY), false);
		}
		return fallen.size();
	}

	private static int recruit(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		String id = StringArgumentType.getString(ctx, "class").toLowerCase(Locale.ROOT);
		if (!ClassCatalog.has(id)) {
			ctx.getSource().sendFailure(Component.literal("Unknown class " + id + "."));
			return 0;
		}
		AllyEntity a = Warbands.spawn(Compat.level(p), ClassCatalog.get(id), level, spot(p), p);
		if (a == null) {
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal(a.record().fullName() + " joins your warband."), true);
		return 1;
	}

	private static int sellsword(CommandContext<CommandSourceStack> ctx, String id, int level) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		if (id == null) {
			AllyEntity a = Sellswords.trySpawn(Compat.level(p), p);
			if (a == null) {
				ctx.getSource().sendFailure(Component.literal("No village bell within 96 blocks, or enough sellswords already wait there."));
				return 0;
			}
			ctx.getSource().sendSuccess(() -> Component.literal(a.record().fullName() + " is waiting near the village bell."), true);
			return 1;
		}
		if (!ClassCatalog.has(id)) {
			ctx.getSource().sendFailure(Component.literal("Unknown class " + id + "."));
			return 0;
		}
		int lvl = level > 0 ? level : Sellswords.rollLevel(Compat.level(p), p);
		AllyEntity a = Warbands.spawn(Compat.level(p), ClassCatalog.get(id), lvl, spot(p), null);
		return a == null ? 0 : 1;
	}

	private static int setLevel(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		LivingEntity looked = Fellowship.lookedAt(p, 16);
		if (!(looked instanceof AllyEntity a)) {
			for (AllyEntity mine : Warbands.liveAllies(p.getUUID())) {
				if (p.distanceToSqr(mine) < 16 * 16) {
					looked = mine;
					break;
				}
			}
		}
		if (!(looked instanceof AllyEntity ally)) {
			ctx.getSource().sendFailure(Component.literal("Look at an ally."));
			return 0;
		}
		ally.record().level = level;
		ally.record().xp = 0;
		ally.applyStats(true);
		ally.refreshNameplate();
		ctx.getSource().sendSuccess(() -> Component.literal(ally.record().name + " is now level " + level + "."), true);
		return 1;
	}

	private static Vec3 spot(ServerPlayer p) {
		Vec3 look = p.getLookAngle();
		return p.position().add(look.x * 2, 0, look.z * 2);
	}
}
