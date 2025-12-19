// MongoDB initialization script for restaurant ordering system

// Switch to the restaurant_app database
db = db.getSiblingDB('restaurant_app');

// Create collections with validation schemas

// Users collection
db.createCollection("users", {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["userId", "email", "profile"],
         properties: {
            userId: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            email: {
               bsonType: "string",
               pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
               description: "must be a valid email address and is required"
            },
            profile: {
               bsonType: "object",
               required: ["firstName", "lastName"],
               properties: {
                  firstName: {
                     bsonType: "string",
                     description: "must be a string and is required"
                  },
                  lastName: {
                     bsonType: "string",
                     description: "must be a string and is required"
                  },
                  phone: {
                     bsonType: "string",
                     description: "must be a string if the field exists"
                  }
               }
            }
         }
      }
   }
});

// Restaurants collection
db.createCollection("restaurants", {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["restaurantId", "name", "cuisine", "address"],
         properties: {
            restaurantId: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            name: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            cuisine: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            address: {
               bsonType: "object",
               required: ["street", "city", "zipCode"],
               description: "must be an object and is required"
            },
            isActive: {
               bsonType: "bool",
               description: "must be a boolean if the field exists"
            }
         }
      }
   }
});

// Carts collection
db.createCollection("carts", {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["cartId", "customerId", "restaurantId", "items"],
         properties: {
            cartId: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            customerId: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            restaurantId: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            items: {
               bsonType: "array",
               description: "must be an array and is required"
            },
            totalAmount: {
               bsonType: "number",
               minimum: 0,
               description: "must be a positive number if the field exists"
            }
         }
      }
   }
});

// Create indexes for better performance

// Users collection indexes
db.users.createIndex({ "userId": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "createdAt": 1 });

// Restaurants collection indexes
db.restaurants.createIndex({ "restaurantId": 1 }, { unique: true });
db.restaurants.createIndex({ "name": 1 });
db.restaurants.createIndex({ "cuisine": 1 });
db.restaurants.createIndex({ "address.city": 1 });
db.restaurants.createIndex({ "isActive": 1 });

// Carts collection indexes
db.carts.createIndex({ "cartId": 1 }, { unique: true });
db.carts.createIndex({ "customerId": 1 });
db.carts.createIndex({ "restaurantId": 1 });
db.carts.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 }); // TTL index for cart expiration

print("MongoDB collections and indexes created successfully for restaurant ordering system");