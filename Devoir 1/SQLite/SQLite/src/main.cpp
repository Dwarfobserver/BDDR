
#include <iostream>
#include <string>
#include <fstream>

#include "json.hpp"
#include "sqlite3.h"

using json = nlohmann::json;
using namespace std::string_literals;

// Used to defer a function's execution at the end of the current scope
namespace detail {
	template <class F>
	class defer_t : F {
	public:
		explicit defer_t(F&& f) : F(std::move(f)) {}
		~defer_t() { F::operator()(); }
	};
} // Unsafe code (no copy elision garanteed => 2 dtor, forward reference then move)
template <class F>
[[nodiscard]]
auto defer(F&& f) { return detail::defer_t<F>{std::move(f)}; }

char const* database_location = "../TP1.db";
char const* spells_location = "../../spells.json";

int main() {
	try {
		sqlite3* db {};

		// Close the database at the end of the scope
		const auto closeDb = defer([&] { sqlite3_close(db); });

		// Open or create the database
		if (sqlite3_open(database_location, &db))
			throw std::runtime_error{ sqlite3_errmsg(db) };

		// Function used to throw the error message when a SQL query fails
		char* errorMessage{};
		const auto checkError = [&errorMessage] {
			if (errorMessage) {
				std::runtime_error err{ errorMessage };
				sqlite3_free(errorMessage);
				throw err;
			}
		};

		// Create the table
		sqlite3_exec(db, "CREATE TABLE IF NOT EXISTS spells ("
			"name TEXT PRIMARY KEY, "
			"level INTEGER, "
			"components VARCHAR(3), "
			"spell_resistance INTEGER"
			")",
			nullptr, nullptr, &errorMessage);
		checkError();

		// Get the number of spells
		int spellsCount;
		sqlite3_exec(db, "SELECT COUNT (*) FROM spells",
			[](void* pResult, int argc, char **argv, char **azColName) -> int {

				auto& result = *static_cast<int*>(pResult);
				result = std::stoi(argv[0]);
				return 0;

		}, &spellsCount, &errorMessage);
		checkError();

		// If no spells are contained, fill table's spells
		if (spellsCount == 0) {
			std::cout << "Spells table empty, begin spells loading ...\r";

			// Get JSON spells
			json jSpells;
			{
				std::ifstream spellsFile{ spells_location };
				if (!spellsFile.is_open()) throw std::runtime_error
					{ "Could not open JSON spells file at "s + spells_location };

				spellsFile >> jSpells;
			}
			// Create SQL 'INSERT' queries
			std::string insertions;
			const auto stringSum = [](std::string const& s1, std::string const& s2) { return s1 + s2; };
			for (json const& jSpell : jSpells) {
				auto const& jComponents = jSpell["components"];

				insertions += "INSERT INTO spells(name, level, components, spell_resistance) VALUES(" +
					jSpell["name"].dump() + ", " +
					jSpell["level"].dump() + ", \"" +
					accumulate(jComponents.begin(), jComponents.end(), ""s, stringSum) + "\", " +
					std::to_string(static_cast<int>(jSpell["spell_resistance"])) +
				");\n";
			}
			// Submit the queries
			sqlite3_exec(db, insertions.c_str(), nullptr, nullptr, &errorMessage);
			checkError();
			std::cout << "Table created : " << jSpells.size() << " spells added\n";
		}

		// Get the usable spell's names
		std::vector<std::string> spellNames;
		sqlite3_exec(db, "SELECT name FROM spells WHERE level <= 4 AND components = 'V'",
			[](void* pResult, int argc, char **argv, char **azColName) -> int {

				auto& spells = *static_cast<std::vector<std::string>*>(pResult);
				spells.push_back(argv[0]);
				return 0;

		}, &spellNames, &errorMessage);
		checkError();

		std::cout << spellNames.size() << " spells usable :\n";
		for (auto const& name : spellNames) {
			std::cout << "  - " << name << '\n';
		}
		std::cout << '\n';
	}
	// Failure ending
	catch (std::exception& e) {
		std::cout << e.what() << '\n';
		std::cout << "Failure. Press a touch to exit.\n";
		std::cin.get();
		return 1;
	}
	// Success ending
	std::cout << "Success ! Press a touch to exit.\n";
	std::cin.get();
}
