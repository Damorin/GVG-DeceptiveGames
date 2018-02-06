package tracks.singlePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tracks.ArcadeMachine;

/**
 * Created with IntelliJ IDEA. User: Diego Date: 04/10/13 Time: 16:29 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Test {

	public static void main(String[] args) {

		// Available tracks:
		String sampleRandomController = "tracks.singlePlayer.simple.sampleRandom.Agent";
		String simpleRandomController = "tracks.singlePlayer.simple.simpleRandom.Agent";
		String doNothingController = "tracks.singlePlayer.simple.doNothing.Agent";
		String sampleOneStepController = "tracks.singlePlayer.simple.sampleonesteplookahead.Agent";
		String sampleFlatMCTSController = "tracks.singlePlayer.simple.greedyTreeSearch.Agent";

		String sampleMCTSController = "tracks.singlePlayer.advanced.sampleMCTS.Agent";
		String sampleRSController = "tracks.singlePlayer.advanced.sampleRS.Agent";
		String sampleRHEAController = "tracks.singlePlayer.advanced.sampleRHEA.Agent";
		String sampleOLETSController = "tracks.singlePlayer.advanced.olets.Agent";
		String sampleGAController = "tracks.singlePlayer.deprecated.sampleGA.Agent";

		// Deception test agents
		String adrienctx = "tracks.singlePlayer.agentsForDeceptiveGames.adrienctx.Agent";
		String AIJim = "tracks.singlePlayer.agentsForDeceptiveGames.AIJim.Agent";
		String aStar = "tracks.singlePlayer.agentsForDeceptiveGames.aStar.Agent";
		String atheneAI = "tracks.singlePlayer.agentsForDeceptiveGames.AtheneAI.Agent";
		String bladerunner = "tracks.singlePlayer.agentsForDeceptiveGames.bladerunner.bladeRunner.Agent";
		String bfs = "tracks.singlePlayer.agentsForDeceptiveGames.breadthFirstSearch.Agent";
		String catLinux = "tracks.singlePlayer.agentsForDeceptiveGames.CatLinux.Agent";
		String catLinux3 = "tracks.singlePlayer.agentsForDeceptiveGames.CatLinux3.Agent";
		String catLinux4 = "tracks.singlePlayer.agentsForDeceptiveGames.CatLinux4.Agent";
		String dfs = "tracks.singlePlayer.agentsForDeceptiveGames.depthFirstSearch.Agent";
		String evoStrats = "tracks.singlePlayer.agentsForDeceptiveGames.evolutionStrategies.Agent";
		String greedySearch = "tracks.singlePlayer.agentsForDeceptiveGames.greedySearch.Agent";
		String hillClimber = "tracks.singlePlayer.agentsForDeceptiveGames.hillClimber.Agent";
		String IceLab = "tracks.singlePlayer.agentsForDeceptiveGames.ICELab.Agent";
		String iterativeDeepening = "tracks.singlePlayer.agentsForDeceptiveGames.iterativeDeepening.Agent";
		String jaydee = "tracks.singlePlayer.agentsForDeceptiveGames.jaydee.Agent";
		String maastCTS2 = "tracks.singlePlayer.agentsForDeceptiveGames.MaastCTS2.Agent";
		String mh2015 = "tracks.singlePlayer.agentsForDeceptiveGames.MH2015.Agent";
		String mnMCTS = "tracks.singlePlayer.agentsForDeceptiveGames.MnMCTS.Agent";
		String muzzle = "tracks.singlePlayer.agentsForDeceptiveGames.muzzle.Agent";
		String mrtndwrd = "tracks.singlePlayer.agentsForDeceptiveGames.mrtndwrd.Agent";
		String novelTS = "tracks.singlePlayer.agentsForDeceptiveGames.NovelTS.Agent";
		String novtea = "tracks.singlePlayer.agentsForDeceptiveGames.NovTea.Agent";
		String number27 = "tracks.singlePlayer.agentsForDeceptiveGames.Number27.Agent";
		String return42 = "tracks.singlePlayer.agentsForDeceptiveGames.Return42.Agent";
		String roskvist = "tracks.singlePlayer.agentsForDeceptiveGames.roskvist.Agent";
		String rooot = "tracks.singlePlayer.agentsForDeceptiveGames.Rooot.Agent";
		String simulatedAnnealing = "tracks.singlePlayer.agentsForDeceptiveGames.simulatedAnnealing.Agent";
		String sja86 = "tracks.singlePlayer.agentsForDeceptiveGames.SJA86.Agent";
		String sja862 = "tracks.singlePlayer.agentsForDeceptiveGames.SJA862.Agent";
		String teamtopbug = "tracks.singlePlayer.agentsForDeceptiveGames.TeamTopbug.Agent";
		String thorbjrn = "tracks.singlePlayer.agentsForDeceptiveGames.thorbjrn.Agent";
		String tomVodo = "tracks.singlePlayer.agentsForDeceptiveGames.TomVodo.Agent";
		String tovo1 = "tracks.singlePlayer.agentsForDeceptiveGames.ToVo1.Agent";
		String ybcriber = "tracks.singlePlayer.agentsForDeceptiveGames.YBCriber.Agent";
		String yolobot = "tracks.singlePlayer.agentsForDeceptiveGames.YOLOBOT.Agent";

		List<String> agents = new ArrayList<>();
		agents.add(adrienctx);
		agents.add(AIJim);
		agents.add(aStar);
		agents.add(atheneAI);
		agents.add(bladerunner);
		agents.add(bfs);
		agents.add(catLinux);
		agents.add(catLinux3);
		agents.add(catLinux4);
		agents.add(dfs);
		agents.add(evoStrats);
		agents.add(greedySearch);
		agents.add(hillClimber);
		agents.add(IceLab);
		agents.add(iterativeDeepening);
		agents.add(jaydee);
		agents.add(maastCTS2);
		agents.add(mh2015);
		agents.add(mnMCTS);
		agents.add(mrtndwrd);
		agents.add(muzzle);
		agents.add(novelTS);
		agents.add(novtea);
		agents.add(number27);
		agents.add(rooot);
		agents.add(roskvist);
		agents.add(simulatedAnnealing);
		agents.add(sja86);
		agents.add(sja862);
		agents.add(teamtopbug);
		agents.add(thorbjrn);
		agents.add(tomVodo);
		agents.add(tovo1);
		agents.add(ybcriber);
		agents.add(yolobot);
		agents.add(return42);

		// Available games:
		String gridGamesPath = "examples/gridphysics/";
		String contGamesPath = "examples/contphysics/";
		String gamesPath;
		String games[];
		String deceptiveGames[];
		boolean GRID_PHYSICS = true;

		// All public games (gridphysics)
		if (GRID_PHYSICS) {
			gamesPath = gridGamesPath;
			games = new String[] { "aliens", "angelsdemons", "assemblyline", "avoidgeorge", "bait", // 0-4
					"beltmanager", "blacksmoke", "boloadventures", "bomber", "bomberman", // 5-9
					"boulderchase", "boulderdash", "brainman", "butterflies", "cakybaky", // 10-14
					"camelRace", "catapults", "chainreaction", "chase", "chipschallenge", // 15-19
					"clusters", "colourescape", "chopper", "cookmepasta", "cops", // 20-24
					"crossfire", "defem", "defender", "digdug", "dungeon", // 25-29
					"eighthpassenger", "eggomania", "enemycitadel", "escape", "factorymanager", // 30-34
					"firecaster", "fireman", "firestorms", "freeway", "frogs", // 35-39
					"garbagecollector", "gymkhana", "hungrybirds", "iceandfire", "ikaruga", // 40-44
					"infection", "intersection", "islands", "jaws", "killBillVol1", // 45-49
					"labyrinth", "labyrinthdual", "lasers", "lasers2", "lemmings", // 50-54
					"missilecommand", "modality", "overload", "pacman", "painter", // 55-59
					"pokemon", "plants", "plaqueattack", "portals", "racebet", // 60-64
					"racebet2", "realportals", "realsokoban", "rivers", "roadfighter", // 65-69
					"roguelike", "run", "seaquest", "sheriff", "shipwreck", // 70-74
					"sokoban", "solarfox", "superman", "surround", "survivezombies", // 75-79
					"tercio", "thecitadel", "thesnowman", "waitforbreakfast", "watergame", // 80-84
					"waves", "whackamole", "wildgunman", "witnessprotection", "wrapsokoban", // 85-89
					"zelda", "zenpuzzle", "decepticoins", "deceptizelda", // 90-93
					"sistersavior", "waferthinmints" }; // 94, 95

		} else {
			gamesPath = contGamesPath;
			games = new String[] { "artillery", "asteroids", "bird", "bubble", "candy", // 0
																						// -
																						// 4
					"lander", "mario", "pong", "ptsp", "racing" }; // 5 - 9
		}

		// Other settings
		boolean visuals = true;
		int seed = new Random().nextInt();

		// Game and level to play
		int gameIdx = 4;
		int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).
		// String game = gamesPath + games[gameIdx] + ".txt";
		// String level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx + ".txt";

		deceptiveGames = new String[] { "decepticoins", "deceptizelda", "sistersavior", "butterflies", "invest",
				"flower", "waferthinmints", "waferthinmintsexit" }; // 0 - 6

		String game = gamesPath + deceptiveGames[gameIdx] + ".txt";
		String level1 = gamesPath + deceptiveGames[gameIdx] + "_lvl" + levelIdx + ".txt";

		String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"
		// + levelIdx + "_" + seed + ".txt";
		// where to record the actions
		// executed. null if not to save.

		// 1. This starts a game, in a level, played by a human.
		ArcadeMachine.playOneGame(game, level1, recordActionsFile, seed);

		// 2. This plays a game in a level by the controller.
		// for (String agent : agents) {
		// System.out.println(agent);
		// int M = 10;
		// for (int i = 0; i < M; i++) {
		// ArcadeMachine.runOneGame(game, level1, visuals, agent, recordActionsFile,
		// seed, 0);
		// }
		// System.out.println(agent + " Fin");
		// }

	}
}
