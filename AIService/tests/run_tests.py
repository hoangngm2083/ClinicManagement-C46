#!/usr/bin/env python3
"""
Python script to run AI Service tests with better output formatting
"""
import sys
import subprocess
import argparse
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description="Run AI Service tests")
    parser.add_argument(
        "--coverage",
        action="store_true",
        help="Run tests with coverage report"
    )
    parser.add_argument(
        "--verbose", "-v",
        action="store_true",
        help="Run tests in verbose mode"
    )
    parser.add_argument(
        "--quiet", "-q",
        action="store_true",
        help="Run tests in quiet mode"
    )
    parser.add_argument(
        "--file",
        help="Run specific test file"
    )
    parser.add_argument(
        "--test",
        help="Run specific test function"
    )
    parser.add_argument(
        "--markers",
        help="Run tests with specific markers (e.g., 'integration')"
    )
    
    args = parser.parse_args()
    
    # Build pytest command
    cmd = ["pytest"]
    
    if args.coverage:
        cmd.extend(["--cov=app", "--cov-report=html", "--cov-report=term"])
    
    if args.verbose:
        cmd.extend(["-vv", "--tb=long"])
    elif args.quiet:
        cmd.append("-q")
    else:
        cmd.extend(["-v", "--tb=short"])
    
    if args.file:
        cmd.append(f"tests/{args.file}")
    elif args.test:
        cmd.append(f"-k {args.test}")
    else:
        cmd.append("tests/")
    
    if args.markers:
        cmd.extend(["-m", args.markers])
    
    # Print command
    print("🧪 Running AI Service Tests")
    print("=" * 50)
    print(f"Command: {' '.join(cmd)}")
    print("=" * 50)
    print()
    
    # Run tests
    try:
        result = subprocess.run(cmd, check=False)
        sys.exit(result.returncode)
    except KeyboardInterrupt:
        print("\n❌ Tests interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error running tests: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()

