RAINAPP - CURRENT ARCHITECTURE (BEFORE REFACTOR)
==================================================
Generated as a snapshot of the existing code, before the planned refactor:
  - handle an input-file format change
  - escape literal "|" found in raw field values (replace with "_") before
    writing pipe-delimited output
  - add input-vs-output record-count verification, split by credit/debit
This document describes CURRENT behavior only. No code was changed to
produce this file.


0. TOP-LEVEL SHAPE
------------------
Single-module Gradle/Java project, no test directory, no existing unit
tests. Entry point: src/main/java/org/example/Main.java.

Per-vendor bank statement files are read from a configured input
directory and processed independently per vendor, then (for the vendors
not commented out) aggregated together.

Vendors (enum AccountVender): wellsfargo, wellsfargomanual, discovery,
chase, citi, boa (boa unused). Each vendor has its own Service class
with its own ad-hoc parsing:
  WfService, WfManualInputService, ChaseService, CitiService,
  DiscoverService  (orchestrated by FileService).

KNOWN ISSUE - input path mismatch:
  Cons.java hardcodes input/output dirs as "./docs/<taxYear>/input" and
  "./docs/<taxYear>/output", but the sample data actually present in the
  repo lives at "./2025/input/*" (repo root, not under docs/). As currently
  configured, the app will not find the sample files. This is presumably
  part of what "input format/location will change" needs to address.

KNOWN ISSUE - Citi bug:
  CitiService.getRecordInfo() instantiates RecordConfigChase instead of a
  Citi-specific config class; no RecordConfigCiti class exists at all.
  Citi is also currently commented out of the vendor list in
  FileService.processFiles() ("2024 citi card bypass as no taxable
  transaction").

KNOWN ISSUE - duplicate-write bug:
  In FileService.processRawFile(), the "_3.csv" step appends
  x.toLineInGeneral() onto the SAME list (linesInFile1) that was already
  written out to "_1.csv", instead of building a fresh list. The result is
  that "_3.csv" ends up containing both the _1 lines and the general lines
  duplicated together.

KNOWN ISSUE - no existing | escaping:
  Nowhere in the codebase is a literal "|" character in a raw field value
  escaped/replaced before being written into the "|"-delimited output.
  If a description (or any field) naturally contains "|", it will silently
  corrupt column alignment in every downstream file.

KNOWN ISSUE - no existing credit/debit classification:
  RecordInfo has only a single signed "amount" field; there is no
  credit/debit/withdraw/saving classification anywhere in the domain model
  or services today. The one vendor whose raw format already separates
  this concept is Citi, which has distinct "Debit" and "Credit" columns in
  its raw CSV - currently unused/uncaptured by CitiService's broken config.
  wf/chase/discovery encode the sign in a single Amount column (Discover
  additionally negates the amount, since Discover shows expenses as a
  positive number).

KNOWN ISSUE - no existing verification/reconciliation:
  Nothing in the codebase counts or compares record counts between raw
  input and any of the output files.


1. PIPELINE LAYERS
-------------------
Processing is NOT implemented as clean, named, streaming pipeline stages.
It is an imperative sequence of steps inside FileService, with the
intermediate result of each step written to disk and (for some steps)
immediately re-read back from disk before the next step runs. Below, each
layer is described with: what it receives as input, what granularity it
operates over, and what (if anything) it writes to disk.

Driven from: FileService.processFiles()  [loops over all configured
vendors, see KNOWN ISSUE above for which vendors are actually enabled]


LAYER 1 - Raw file discovery
  Code: FileService.processRawFile(), dir.listFiles(...)
  Input: a vendor's filename prefix (e.g. "wf_", "Chase_", "DFS_") and the
         configured input directory.
  Granularity: one vendor's set of files on disk (file names only, no
         content read yet).
  Output to disk: none.

LAYER 2 - Raw line read
  Code: Files.readAllLines(myPath, UTF_8), inside the per-file loop in
         processRawFile()
  Input: one physical input file path.
  Granularity: ONE FILE at a time -> all of that file's raw lines, in
         memory as List<String>. (Each vendor's files are processed one
         file at a time, in a loop; nothing holds more than one file's raw
         lines in memory at once at this layer.)
  Output to disk: none (held in memory as `lines`).

LAYER 3 - Per-vendor reformat to pipe-delimited ("_0" layer)
  Code: FileService.reformatLines() dispatches by vendor to
         WfService/ChaseService/CitiService/DiscoverService
         .putALlInOneFile(fileName, lines)
  Input: one file's raw lines (from Layer 2) + the vendor's parsing rules
         (each vendor does its own CSV/quote handling - inconsistently;
         see KNOWN ISSUEs above: Discover uses quote-aware regex split,
         Chase/Citi do a naive comma->pipe replace with no quote
         awareness, Wf strips quotes manually).
  Granularity: ONE FILE's lines at a time, line-by-line within that file.
  Output to disk: YES - appended immediately, per file, to
         <output>/<outFilePrefix>_<vendor>_<taxYear>_0.csv
         (one shared "_0.csv" per vendor, across all of that vendor's
         input files - each file's reformatted lines are appended as that
         file is processed).

LAYER 4 - Re-read "_0" file, parse into RecordInfo ("_1" layer)
  Code: processRawFile() re-reads outFile0 in full via
         Files.readAllLines(), then FileService.getRecordInfos()
         dispatches by vendor to ...Service.getRecordInfo(allLines)
  Input: ALL pipe-delimited lines for ALL of that vendor's files combined
         (the full "_0.csv" content just written in Layer 3 - note this is
         a disk round-trip, not a direct in-memory handoff from Layer 3).
  Granularity: all records for one vendor at once (every line from every
         file belonging to that vendor), producing List<RecordInfo> -
         parsing is done line-by-line within that combined list, splitting
         each line on "|" and reading hardcoded column indices.
  Output to disk: YES - each RecordInfo.toALine() appended to
         <output>/<outFilePrefix>_<vendor>_<taxYear>_1.csv

LAYER 5 - Grouped summary ("_2" layer)
  Code: HelperService.processSummary(recordInfos)
  Input: the full List<RecordInfo> for one vendor (from Layer 4, in
         memory - no re-read here).
  Granularity: all records for one vendor at once, grouped by
         (group1, group2) category.
  Output to disk: YES - appended to
         <output>/<outFilePrefix>_<vendor>_<taxYear>_2.csv

LAYER 6 - "General" lines ("_3" layer)  [see KNOWN ISSUE: duplicate-write bug]
  Code: processRawFile(), recordInfos.forEach(x ->
         linesInFile1.add(x.toLineInGeneral()))
  Input: the same List<RecordInfo> from Layer 4 (in memory).
  Granularity: all records for one vendor at once.
  Output to disk: YES - but written into the REUSED linesInFile1 list
         (already containing Layer 4's _1 lines), so
         <output>/<outFilePrefix>_<vendor>_<taxYear>_3.csv ends up with
         both the _1-style lines and the general lines appended together.
  Returns: processRawFile() returns this vendor's List<RecordInfo> up to
         the caller (FileService.processFiles()).

LAYER 7 - Cross-vendor aggregation (group sums)
  Code: FileService.processFiles(), after collecting all vendors'
         RecordInfo into allRecordInfos
  Input: ALL vendors' RecordInfo combined, held fully in memory
         (allRecordInfos - every record from every processed vendor's
         Layer 4 output).
  Granularity: all records, all vendors, at once.
  Output to disk: YES, three separate files, each a full pass over
         allRecordInfos with different formatting:
    a) all_<outFilePrefix>withGroupSum.csv
       - one line per record, with that record's (group1,group2) sum
         attached (toLineWithGroupAmmount())
    b) all_<outFilePrefix>withGroupSumCount.csv
       - one line per record, with that record's (group1,group2) sum AND
         count attached (toLineWithGroupSumAndCount())
    c) all_<outFilePrefix>summary.csv
       - one line per distinct (group1,group2) group (not per record):
         group1|group2|groupSum|groupCount

LAYER 8 - Combine per-vendor summaries into one file
  Code: FileService.summaryInOne(outFilePrefix)
  Input: every per-vendor "_2.csv" file on disk matching the current run's
         prefix (re-reads each file's lines from disk - this is a second,
         separate aggregation pass, independent of Layer 7, operating only
         on the "_2" (grouped summary) files rather than on RecordInfo
         objects in memory).
  Granularity: one file at a time, all lines of that file, appended in
         sequence across all matching files.
  Output to disk: YES - appended to all_<outFilePrefix>.csv


2. SUMMARY TABLE
----------------
Layer | Operates over            | Reads from        | Writes to disk
------|--------------------------|--------------------|-------------------
1     | file names (one vendor)  | input dir listing   | no
2     | one file's raw lines     | one file on disk    | no
3     | one file's lines         | in-memory (Layer 2) | <vendor>_0.csv (append per file)
4     | all lines, one vendor    | re-read <vendor>_0.csv | <vendor>_1.csv
5     | all records, one vendor  | in-memory (Layer 4) | <vendor>_2.csv
6     | all records, one vendor  | in-memory (Layer 4) | <vendor>_3.csv (bug: duplicates _1 content)
7     | all records, all vendors | in-memory (all vendors' Layer 4 results) | all_*withGroupSum.csv, all_*withGroupSumCount.csv, all_*summary.csv
8     | one _2.csv file at a time| re-read every vendor's _2.csv from disk | all_<prefix>.csv


3. OUTPUT FILE DELIMITER
------------------------
All output lines are "|"-delimited. No escaping of a literal "|" inside a
raw field value exists anywhere in the current code (see KNOWN ISSUE
above).
