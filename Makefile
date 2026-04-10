# Drinks POS v2 Makefile

# Variables
LIB_DIR = lib
OUT_DIR = out
DB_DIR = db
SRC_DIR = src
REPO_DIR = src/repo
WEB_DIR = web
JAR_SQLITE = $(LIB_DIR)/sqlite-jdbc-3.51.3.0.jar
JAR_SLF4J_API = $(LIB_DIR)/slf4j-api-1.7.32.jar
JAR_SLF4J_SIMPLE = $(LIB_DIR)/slf4j-simple-1.7.32.jar

# Classpaths
# Note: Using : for Linux/macOS. For Windows, one would normally use ; but Make is mostly used on Unix-like systems.
CP = "$(OUT_DIR):$(JAR_SQLITE):$(JAR_SLF4J_API):$(JAR_SLF4J_SIMPLE)"

.PHONY: all compile run clean dist help

all: compile

help:
	@echo "Available commands:"
	@echo "  make compile  - Compile the Java source code"
	@echo "  make run      - Run the POS server"
	@echo "  make clean    - Remove generated 'out' and 'db' directories"
	@echo "  make dist     - Create a zip archive for distribution (excludes generated files)"

compile:
	mkdir -p $(OUT_DIR)
	javac -cp "$(JAR_SQLITE)" -d $(OUT_DIR) $(SRC_DIR)/*.java $(REPO_DIR)/*.java

run: compile
	java -cp $(CP) App

clean:
	rm -rf $(OUT_DIR) $(DB_DIR)

dist: clean
	@echo "Creating drinks-pos-v2.zip..."
	zip -r drinks-pos-v2.zip src/ web/ lib/ *.sh *.bat Makefile *.md
	@echo "Done. drinks-pos-v2.zip created (excluding generated build/db files)."
