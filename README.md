ML Tools
========

# ml-tools
Tools that help with Machine Learning Tasks
- export BigQuery Tables to svmlite rows, labeled-points or tensorflow records
- train XGB models from tables or files
- test trained XGB models

## Why
Transforms datasets to formats that  ML  Models understand.
Also lightweing wrapper around XGBoost that helps with training and testing

## Installing
```
sbt assembly
./scripts//ml-tools
```

## Usage
```
Usage: ml-tools [export|test|train] options...

Options:

  -h, --help      Show help message
  -v, --version   Show version of this program

Subcommand: export (alias: ex)
  -f, --format  <arg>     Choices: svm, lp, tf
  -h, --hashsize  <arg>
  -i, --input  <arg>
  -l, --label  <arg>
  -o, --output  <arg>
      --help              Show help message

Subcommand: train (alias: tr)
  -h, --hashsize  <arg>
  -i, --input  <arg>
  -l, --label  <arg>
  -m, --model  <arg>
      --help              Show help message

Subcommand: test (alias: te)
  -h, --hashsize  <arg>
  -i, --input  <arg>
  -l, --label  <arg>
  -m, --model  <arg>
      --help              Show help message
```
