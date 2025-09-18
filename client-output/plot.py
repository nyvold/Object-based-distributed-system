#!/usr/bin/env python3

import re
import os
import glob
import pandas as pd
import matplotlib.pyplot as plt

def plot_turnaround_times(data_file_path, output_filename, plot_title):
    """
    Parses a given output file for turnaround times and generates a plot.
    This version is updated to look for "TurnMs:XXX".
    """
    print(f"-> Generating plot for: {plot_title}")
    
    if not os.path.exists(data_file_path):
        print(f"   - Warning: Data file not found at '{data_file_path}'. Skipping.")
        return

    query_numbers = []
    turnaround_times = []
    
    # --- THIS IS THE CORRECTED LINE ---
    # It now looks for "TurnMs:169" instead of "turnaround time: 169 ms"
    time_pattern = re.compile(r"TurnMs:(\d+)")

    with open(data_file_path, 'r') as f:
        for i, line in enumerate(f):
            # Skip comment lines
            if line.startswith('#'):
                continue
            
            match = time_pattern.search(line)
            if match:
                # The first captured group `(\d+)` is the number
                time_ms = int(match.group(1))
                turnaround_times.append(time_ms)
                query_numbers.append(len(query_numbers) + 1) # Use an independent counter for valid lines
    
    if not turnaround_times:
        print(f"   - Warning: No turnaround times found in '{data_file_path}'. Skipping plot.")
        return

    plt.figure(figsize=(12, 6))
    plt.plot(query_numbers, turnaround_times, marker='.', linestyle='-', markersize=4)
    plt.title(plot_title)
    plt.xlabel('Query Number (valid entries only)')
    plt.ylabel('Turnaround Time (ms)')
    plt.grid(True, which='both', linestyle='--', linewidth=0.5)
    plt.tight_layout()
    
    plt.savefig(output_filename)
    plt.close() # Close the figure to free up memory
    print(f"   - ✅ Saved as '{output_filename}'")


def plot_server_queues(scenario_path, output_filename, plot_title):
    """
    Finds all server queue logs in a directory and plots their size over time.
    """
    print(f"-> Generating plot for: {plot_title}")
    
    pattern = os.path.join(scenario_path, 'server*_queue.csv')
    queue_files = glob.glob(pattern)

    if not queue_files:
        print(f"   - Warning: No server queue files found in '{scenario_path}'. Skipping.")
        return

    plt.figure(figsize=(12, 6))
    
    for file in queue_files:
        try:
            server_name = os.path.basename(file).split('_')[0]
            df = pd.read_csv(file, header=None, names=['timestamp', 'queue_size'])
            plt.plot(df['timestamp'], df['queue_size'], label=f'{server_name} Queue')
        except Exception as e:
            print(f"   - Warning: Could not process file {file}: {e}")
            continue
            
    plt.title(plot_title)
    plt.xlabel('Unix Timestamp')
    plt.ylabel('Queue Size')
    plt.legend()
    plt.grid(True, which='both', linestyle='--', linewidth=0.5)
    plt.tight_layout()
    
    plt.savefig(output_filename)
    plt.close() # Close the figure to free up memory
    print(f"   - ✅ Saved as '{output_filename}'")


if __name__ == '__main__':
    # This script assumes it is run from the `client-output` directory
    base_dirs = ['20 ms delay', '50 ms delay']
    scenarios = {
        "no cache on both": "naive_server.txt",
        "server cache only": "naive_server.txt",
        "client cache only": "client_cache.txt" 
    }

    print("--- Starting Plot Generation ---")
    
    for delay_dir in base_dirs:
        # Check if the delay directory exists to avoid errors
        if not os.path.isdir(delay_dir):
            print(f"\nSkipping directory (not found): {delay_dir}")
            continue

        for scenario_name, data_filename in scenarios.items():
            
            print(f"\nProcessing: {delay_dir} / {scenario_name}")
            
            scenario_path = os.path.join(delay_dir, scenario_name)
            
            # --- Turnaround Time Plot ---
            turnaround_data_file = os.path.join(scenario_path, data_filename)
            delay_tag = delay_dir.replace(" ", "_")
            scenario_tag = scenario_name.replace(" ", "_")
            
            tt_plot_title = f"Turnaround Time ({delay_dir}, {scenario_name})"
            tt_output_file = f"turnaround_time_{delay_tag}_{scenario_tag}.png"
            plot_turnaround_times(turnaround_data_file, tt_output_file, tt_plot_title)
            
            # --- Server Queue Plot ---
            sq_plot_title = f"Server Queue Size ({delay_dir}, {scenario_name})"
            sq_output_file = f"server_queues_{delay_tag}_{scenario_tag}.png"
            plot_server_queues(scenario_path, sq_output_file, sq_plot_title)

    print("\n--- All plots generated! ---")