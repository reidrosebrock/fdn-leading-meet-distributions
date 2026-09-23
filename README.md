# Counting by Leading Meet in Free Distributive Lattices

Data and code accompanying the paper:

> *Counting by Leading Meet in Free Distributive Lattices*

## Overview

The free distributive lattice $FD(n)$ has $M(n)$ elements, where $M(n)$ is the $n$th Dedekind number. This repository contains:

- The paper itself (`paper/`)
- Computed distributions of elements of $FD(n)$ by leading meet, for $n = 3$, $\dots$, $7$ (`data/`)
- Source code used to generate the distributions (`code/`)

## Data

Each CSV file has two columns:

- `Meet` — the leading meet, written as a conjunction of generators (e.g., `A∧B∧C`)
- `Count` — number of elements whose canonical join-of-meets representation begins with that meet

The text files beginning with 'fdl_elements' provide a listing of all elements of a free distributive lattice with $n$ generators, ordered by leading meet.

Files:

- `n3_counts.csv` — $FD(3)$
- `n4_counts.csv` — $FD(4)$
- `n5_counts.csv` — $FD(5)$
- `n6_counts.csv` — $FD(6)$
- `n7_counts.csv` — $FD(7)$

The counts exclude $\mathbf{0}$, $\mathbf{1}$, and the meet of all generators $A_1 \wedge \cdots \wedge A_n$. Each file therefore sums to $M(n) - 3$.

Also included are the following:

- `fdl_elements_n3.txt` - $FD(3)$
- `fdl_elements_n4.txt` - $FD(4)$
- `fdl_elements_n5.txt` - $FD(5)$

## Code

The enumeration programs are written in Java. `DedekindLexParallel.java` takes $n$ as input and outputs the counts by leading meet. `FDLEnumerator.java` explicitly enumerates by name each element in a free distributive lattice for a given value of $n$ and writes the results to a text file.

## Citation

If you use this data or code in your work, please cite the paper (see `paper/`).

## Contact

Reid Rosebrock — grosebrock@gmail.com


