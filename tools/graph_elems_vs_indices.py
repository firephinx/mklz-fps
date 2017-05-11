from csv import reader
import matplotlib.pyplot as plt

with open('../data/go/local/elems_10e7_r5.csv', 'r') as f_elems:
    data_elems = list(reader(f_elems))

with open('../data/go/local/indices_10e7_r5.csv', 'r') as f_indices:
    data_indices = list(reader(f_indices))


plt.figure()

plt.xlabel("number of elements")
plt.ylabel("time taken to sort by element")
plt.title("---")
# print data_elems[0]

elems = [int(i[0]) for i in data_elems]
e_totals = [float(i[3]) for i in data_elems]
plt.scatter(elems,e_totals,color='red')

elems = [int(i[0]) for i in data_indices]
sort_indices = [float(i[1]) for i in data_indices]
shuffle_elems = [float(i[2]) for i in data_indices]
i_totals = [float(i[3]) for i in data_indices]
plt.scatter(elems,sort_indices,color='yellow')
plt.scatter(elems,shuffle_elems,color='green')
plt.scatter(elems,i_totals,color='blue')


# Here we process the data to get the average..

# Build a map for each kind of data..
elems_total = {}
indices_total = {}
indices_sort = {}
indices_shuffle = {}
count = {}

for val in elems:
  elems_total[val] = 0.0
  indices_total[val] = 0.0
  indices_sort[val] = 0.0
  indices_shuffle[val] = 0.0
  count[val] = 0

# Sum up
for i, val in enumerate(elems):
  elems_total[val] += e_totals[i]
  indices_total[val] += i_totals[i]
  indices_sort[val] += sort_indices[i]
  indices_shuffle[val] += shuffle_elems[i]
  count[val] += 1

# Compute averages
for key in sorted(elems_total):
  denom = count[key]
  elems_total[key] /= denom
  indices_total[key] /= denom
  indices_sort[key] /= denom
  indices_shuffle[key] /= denom

plt.figure()

x_axis = elems_total.keys()

# print len(x_axis)
# print type(x_axis)
# print len(elems_total.values())
# print type(elems_total.values())
plt.xlabel("number of elements")
plt.ylabel("time taken to sort by element")

plt.scatter(x_axis, elems_total.values(), color="red", label="Sorting by Element")
plt.scatter(x_axis, indices_total.values(), color="blue", label="Sorting by Index")
plt.scatter(x_axis, indices_sort.values(), color="yellow", label="Sorting the Indices")
plt.scatter(x_axis, indices_shuffle.values(), color="green", label="Permuting the Elements")

plt.legend()

plt.show()
